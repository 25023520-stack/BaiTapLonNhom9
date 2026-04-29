package com.auction.system.manager;

import com.auction.system.model.auction.AuctionStatus;
import com.auction.system.model.auction.Bid;
import com.auction.system.model.user.Bidder;
import com.auction.system.model.item.Item;
import com.auction.system.model.user.Seller;
import com.auction.system.model.user.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AuctionManager {
    private final Map<String, User> usersById = new HashMap<>();
    private final Map<String, User> usersByUsername = new HashMap<>();
    private final Map<String, Item> itemsById = new HashMap<>();

    public synchronized void registerUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        if (usersById.containsKey(user.getId())) {
            throw new IllegalArgumentException("User id already exists");
        }
        if (usersByUsername.containsKey(user.getUserName())) {
            throw new IllegalArgumentException("Username already exists");
        }

        usersById.put(user.getId(), user);
        usersByUsername.put(user.getUserName(), user);
    }

    public synchronized Optional<User> login(String username, String password) {
        User user = usersByUsername.get(username);
        if (user == null || !user.checkPassword(password)) {
            return Optional.empty();
        }
        return Optional.of(user);
    }

    public synchronized void addItem(Item item, Seller seller) {
        validateSeller(seller);
        if (item == null) {
            throw new IllegalArgumentException("Item must not be null");
        }
        if (itemsById.containsKey(item.getId())) {
            throw new IllegalArgumentException("Item id already exists");
        }

        item.setSellerId(seller.getId());
        item.setStatus(AuctionStatus.OPEN);
        item.setCurrentPrice(item.getStartPrice());
        itemsById.put(item.getId(), item);
    }

    public synchronized void updateItem(Item item, Seller seller) {
        validateSeller(seller);
        Item existingItem = requireItem(item.getId());
        if (!seller.getId().equals(existingItem.getSellerId())) {
            throw new IllegalArgumentException("Seller can only update their own item");
        }
        if (existingItem.getStatus() == AuctionStatus.RUNNING) {
            throw new IllegalStateException("Cannot edit an item while auction is running");
        }

        existingItem.setName(item.getName());
        existingItem.setDescription(item.getDescription());
        existingItem.setStartPrice(item.getStartPrice());
        existingItem.setCurrentPrice(Math.max(existingItem.getCurrentPrice(), item.getStartPrice()));
    }

    public synchronized void removeItem(String itemId, Seller seller) {
        validateSeller(seller);
        Item existingItem = requireItem(itemId);
        if (!seller.getId().equals(existingItem.getSellerId())) {
            throw new IllegalArgumentException("Seller can only remove their own item");
        }
        if (!existingItem.getBidHistory().isEmpty()) {
            throw new IllegalStateException("Cannot remove item that already has bids");
        }

        itemsById.remove(itemId);
    }

    public synchronized void startAuction(String itemId, LocalDateTime startTime, LocalDateTime endTime) {
        Item item = requireItem(itemId);
        if (endTime == null || startTime == null || !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Auction end time must be after start time");
        }

        item.setStartTime(startTime);
        item.setEndTime(endTime);
        item.setStatus(AuctionStatus.RUNNING);
    }

    public synchronized void finishAuction(String itemId) {
        Item item = requireItem(itemId);
        item.setStatus(AuctionStatus.FINISHED);
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

        Bid bid = new Bid(bidder.getId(), bidder, bidAmount);
        item.setCurrentPrice(bidAmount);
        item.setHighestBidderId(bidder.getId());
        item.addBid(bid);
        return bid;
    }

    public synchronized List<Item> getAllItems() {
        return itemsById.values()
                .stream()
                .sorted(Comparator.comparingInt(Item::getId))
                .toList();
    }

    public synchronized Optional<Item> findItemById(String itemId) {
        return Optional.ofNullable(itemsById.get(itemId));
    }

    private void validateSeller(Seller seller) {
        if (seller == null) {
            throw new IllegalArgumentException("Seller must not be null");
        }
        User existingSeller = usersById.get(seller.getId());
        if (!(existingSeller instanceof Seller)) {
            throw new IllegalArgumentException("Seller account is not registered");
        }
    }

    private Item requireItem(String itemId) {
        Item item = itemsById.get(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Item does not exist");
        }
        return item;
    }
}
