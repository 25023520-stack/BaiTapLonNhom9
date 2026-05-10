package com.auction.system.server.manager;

import com.auction.system.exception.InvalidDataException;
import com.auction.system.exception.ItemNotFoundException;
import com.auction.system.model.auction.Auction;
import com.auction.system.model.auction.AuctionStatus;
import com.auction.system.model.auction.Bid;
import com.auction.system.model.item.Item;
import com.auction.system.model.user.Bidder;
import com.auction.system.model.user.Seller;
import com.auction.system.model.user.User;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class AuctionManager {
    private static final AuctionManager INSTANCE =
            new AuctionManager(AuthManager.getInstance(), ItemManager.getInstance());

    private final AuthManager authManager;
    private final ItemManager itemManager;
    private final Map<String, Auction> auctionsById = new HashMap<>();

    AuctionManager() {
        this(new AuthManager(false), new ItemManager(false));
    }

    AuctionManager(AuthManager authManager, ItemManager itemManager) {
        this.authManager = authManager;
        this.itemManager = itemManager;
    }

    public static AuctionManager getInstance() {
        return INSTANCE;
    }

    public synchronized void register(User user) {
        registerUser(user);
    }

    public synchronized void registerUser(User user) {
        authManager.registerUser(user);
    }

    public synchronized Optional<User> login(String username, String password) {
        return authManager.login(username, password);
    }

    public synchronized void addItem(Item item, Seller seller) {
        validateSeller(seller);
        if (item == null) {
            throw new IllegalArgumentException("Item must not be null");
        }

        item.setSellerId(seller.getId());
        item.setStatus(AuctionStatus.OPEN);
        item.setCurrentPrice(item.getStartPrice());

        try {
            itemManager.addItem(item);
        } catch (InvalidDataException exception) {
            throw new IllegalArgumentException(exception.getMessage(), exception);
        }
    }

    public synchronized void updateItem(Item item, Seller seller) {
        validateSeller(seller);
        if (item == null) {
            throw new IllegalArgumentException("Item must not be null");
        }

        Item existingItem = requireItem(item.getId());
        if (!Objects.equals(seller.getId(), existingItem.getSellerId())) {
            throw new IllegalArgumentException("Seller can only update their own item");
        }
        if (existingItem.getStatus() == AuctionStatus.RUNNING) {
            throw new IllegalStateException("Cannot edit an item while auction is running");
        }

        try {
            itemManager.updateItem(item.getId(), item.getName(), item.getDescription(), item.getStartPrice());
        } catch (ItemNotFoundException | InvalidDataException exception) {
            throw new IllegalArgumentException(exception.getMessage(), exception);
        }
    }

    public synchronized void removeItem(String itemId, Seller seller) {
        validateSeller(seller);
        Item existingItem = requireItem(itemId);
        if (!Objects.equals(seller.getId(), existingItem.getSellerId())) {
            throw new IllegalArgumentException("Seller can only remove their own item");
        }
        if (!existingItem.getBidHistory().isEmpty()) {
            throw new IllegalStateException("Cannot remove item that already has bids");
        }

        try {
            itemManager.deleteItem(itemId);
            auctionsById.remove(itemId);
        } catch (ItemNotFoundException exception) {
            throw new IllegalArgumentException(exception.getMessage(), exception);
        }
    }

    public synchronized void startAuction(String itemId, LocalDateTime startTime, LocalDateTime endTime) {
        Item item = requireItem(itemId);
        if (endTime == null || startTime == null || !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Auction end time must be after start time");
        }

        item.setStartTime(startTime);
        item.setEndTime(endTime);
        item.setStatus(AuctionStatus.RUNNING);
        auctionsById.put(itemId, new Auction(itemId, item));
    }

    public synchronized void finishAuction(String itemId) {
        Item item = requireItem(itemId);
        item.setStatus(AuctionStatus.FINISHED);

        Auction auction = auctionsById.get(itemId);
        if (auction != null) {
            auction.finishAuction();
        }
    }

    public synchronized Bid placeBid(String itemId, Bidder bidder, double bidAmount) {
        if (bidder == null) {
            throw new IllegalArgumentException("Bidder must not be null");
        }

        Item item = requireItem(itemId);
        if (item.getStatus() != AuctionStatus.RUNNING) {
            throw new IllegalStateException("Auction is not running");
        }
        if (item.getEndTime() != null && LocalDateTime.now().isAfter(item.getEndTime())) {
            item.setStatus(AuctionStatus.FINISHED);
            throw new IllegalStateException("Auction already finished");
        }
        if (bidAmount <= item.getCurrentPrice()) {
            throw new IllegalArgumentException("Bid amount must be greater than current price");
        }

        Bid bid = new Bid("BID-" + UUID.randomUUID(), bidder, bidAmount);
        item.setCurrentPrice(bidAmount);
        item.setHighestBidderId(bidder.getId());
        item.addBid(bid);
        return bid;
    }

    public synchronized List<Item> getAllItems() {
        return itemManager.getAllItems()
                .stream()
                .sorted(Comparator.comparing(Item::getId, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    public synchronized Collection<Auction> getAllAuctions() {
        return auctionsById.values();
    }

    public synchronized Optional<Item> findItemById(String itemId) {
        try {
            return Optional.of(itemManager.findItemById(itemId));
        } catch (ItemNotFoundException exception) {
            return Optional.empty();
        }
    }

    private void validateSeller(Seller seller) {
        if (seller == null) {
            throw new IllegalArgumentException("Seller must not be null");
        }

        Optional<User> existingSeller = authManager.findById(seller.getId());
        if (existingSeller.isEmpty() || !(existingSeller.get() instanceof Seller)) {
            throw new IllegalArgumentException("Seller account is not registered");
        }
    }

    private Item requireItem(String itemId) {
        try {
            return itemManager.findItemById(itemId);
        } catch (ItemNotFoundException exception) {
            throw new IllegalArgumentException("Item does not exist", exception);
        }
    }
}
