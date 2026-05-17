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
import com.auction.system.server.dao.ItemDAO;
import com.auction.system.server.dao.AuctionDAO;
import com.auction.system.server.database.Database;

import java.sql.Connection;
import java.sql.SQLException;
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

    private final ItemDAO itemDAO = new ItemDAO();
    private final AuctionDAO auctionDAO = new AuctionDAO();

    private com.auction.system.server.observer.AuctionSubject auctionSubject;

    private AuctionManager() {
        this(AuthManager.getInstance(), ItemManager.getInstance());
    }

    private AuctionManager(AuthManager authManager, ItemManager itemManager) {
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

        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start time and end time must not be null");
        }

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Auction end time must be after start time");
        }

        String auctionId = "AUC-" + itemId;

        try (Connection conn = Database.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            try {
                boolean itemUpdated = itemDAO.updateAuctionInfo(
                        conn,
                        itemId,
                        AuctionStatus.RUNNING,
                        startTime,
                        endTime
                );

                if (!itemUpdated) {
                    throw new SQLException("Cannot update item auction info");
                }

                boolean auctionSaved;

                if (auctionDAO.existsByItemId(conn, itemId)) {
                    auctionSaved = auctionDAO.updateAuctionTimeAndStatus(
                            conn,
                            itemId,
                            startTime,
                            endTime,
                            AuctionStatus.RUNNING
                    );
                } else {
                    auctionSaved = auctionDAO.insertAuction(
                            conn,
                            auctionId,
                            itemId,
                            startTime,
                            endTime,
                            AuctionStatus.RUNNING
                    );
                }

                if (!auctionSaved) {
                    throw new SQLException("Cannot save auction");
                }

                conn.commit();

                //update RAM sau khi thành công
                item.setStartTime(startTime);
                item.setEndTime(endTime);
                item.setStatus(AuctionStatus.RUNNING);

                Auction auction = auctionsById.get(itemId);
                if (auction == null) {
                    auction = new Auction(auctionId, item);
                    auctionsById.put(itemId, auction);
                }
                auction.setStatus(AuctionStatus.RUNNING);
                if (auctionSubject != null) {
                    auctionSubject.notifyObservers(item, "AUCTION_STARTED");
                }

            } catch (SQLException e) {
                conn.rollback();
                throw new IllegalStateException("Cannot start auction: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Database error when starting auction", e);
        }
    }

    public synchronized void finishAuction(String itemId) {
        Item item = requireItem(itemId);
        item.setStatus(AuctionStatus.FINISHED);

        Auction auction = auctionsById.get(itemId);
        if (auction != null) {
            auction.finishAuction();
        }
        if (auctionSubject != null) {
            auctionSubject.notifyObservers(item, "AUCTION_FINISHED");
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
        return List.copyOf(auctionsById.values());
    }

    public synchronized Optional<Item> findItemById(String itemId) {
        try {
            return Optional.of(itemManager.findItemById(itemId));
        } catch (ItemNotFoundException exception) {
            return Optional.empty();
        }
    }

    synchronized void resetForTest() {
        auctionsById.clear();
        itemManager.resetForTest();
        authManager.resetForTest();
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

    public void setAuctionSubject(com.auction.system.server.observer.AuctionSubject subject) {
        this.auctionSubject = subject;
    }
}
