package com.auction.system.server.manager;

import com.auction.system.exception.InvalidDataException;
import com.auction.system.exception.ItemNotFoundException;
import com.auction.system.common.money.Money;
import com.auction.system.model.auction.Auction;
import com.auction.system.model.auction.AuctionStatus;
import com.auction.system.model.auction.Bid;
import com.auction.system.model.item.Item;
import com.auction.system.model.user.Bidder;
import com.auction.system.model.user.Seller;
import com.auction.system.model.user.User;
import com.auction.system.model.auction.AutoBid;
import com.auction.system.server.dao.AutoBidDAO;
import com.auction.system.server.dao.ItemDAO;
import com.auction.system.server.dao.BidDAO;
import com.auction.system.server.dao.AuctionDAO;
import com.auction.system.server.dao.UserDAO;
import com.auction.system.server.database.Database;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
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
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(AuctionManager.class);
    private static final long ANTI_SNIPING_THRESHOLD_SECONDS = 60;
    private static final long ANTI_SNIPING_EXTENSION_SECONDS = 60;

    private static final AuctionManager INSTANCE =
            new AuctionManager(AuthManager.getInstance(), ItemManager.getInstance());

    private final AuthManager authManager;
    private final ItemManager itemManager;
    private final Map<String, Auction> auctionsById = new HashMap<>();
    private final ConcurrentMap<String, ReentrantLock> itemLocks = new ConcurrentHashMap<>();

    private final ItemDAO itemDAO = new ItemDAO();
    private final AuctionDAO auctionDAO = new AuctionDAO();
    private final BidDAO bidDAO = new BidDAO();
    private final AutoBidDAO autoBidDAO = new AutoBidDAO();
    private final UserDAO userDAO = new UserDAO();

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
        item.setSellerUsername(seller.getUserName());
        item.setCategory(item.getCategory());

        item.setStatus(AuctionStatus.OPEN);
        item.setCurrentPrice(item.getStartPrice());

        try {
            itemManager.addItem(item);
        } catch (InvalidDataException exception) {
            throw new IllegalArgumentException(exception.getMessage(), exception);
        }
        if (auctionSubject != null) {
            auctionSubject.notifyObservers(item, "ITEM_ADDED");
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
        AuctionStatus currentStatus = existingItem.getStatus();
        if (currentStatus == AuctionStatus.RUNNING) {
            throw new IllegalStateException("Cannot edit an item while auction is running");
        }
        if (currentStatus == AuctionStatus.PAID) {
            throw new IllegalStateException("Cannot edit an item with status: " + currentStatus);
        }

        try {
            itemManager.updateItem(item.getId(), item.getName(), item.getDescription(),
                    item.getStartPrice(), item.getImagePath(), item.getCategory());
        } catch (ItemNotFoundException | InvalidDataException exception) {
            throw new IllegalArgumentException(exception.getMessage(), exception);
        }
        if (currentStatus == AuctionStatus.CANCELED || currentStatus == AuctionStatus.FINISHED) {
            Item refreshedItem = requireItem(item.getId());
            refreshedItem.setStatus(AuctionStatus.OPEN);
            refreshedItem.setStartTime(null);
            refreshedItem.setEndTime(null);
            refreshedItem.setAuctionApproved(false);
            refreshedItem.setCurrentPrice(refreshedItem.getStartPrice());
            refreshedItem.setHighestBidderId(null);
            refreshedItem.setHighestBidderUsername(null);
            itemDAO.clearAuctionRequest(refreshedItem.getId());
            auctionsById.remove(refreshedItem.getId());
        }
        Item updatedItem = requireItem(item.getId());
        if (auctionSubject != null) {
            auctionSubject.notifyObservers(updatedItem, "ITEM_UPDATED");
        }
    }

    public synchronized void removeItem(String itemId, Seller seller) {
        validateSeller(seller);
        Item existingItem = requireItem(itemId);
        if (!Objects.equals(seller.getId(), existingItem.getSellerId())) {
            throw new IllegalArgumentException("Seller can only remove their own item");
        }
        AuctionStatus status = existingItem.getStatus();
        if (status == AuctionStatus.RUNNING) {
            throw new IllegalStateException("Cannot remove item with status: " + status);
        }
        Item itemToNotify = existingItem;


        try {
            itemManager.deleteItem(itemId);
            auctionsById.remove(itemId);
        } catch (ItemNotFoundException exception) {
            throw new IllegalArgumentException(exception.getMessage(), exception);
        }
        if (auctionSubject != null) {
            auctionSubject.notifyObservers(itemToNotify, "ITEM_REMOVED");
        }
    }

    public void startAuction(String itemId, LocalDateTime startTime, LocalDateTime endTime) {
        if(itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("Item id must not be empty");
        }

        ReentrantLock lock = lockForItem(itemId);
        lock.lock();

        try{
            Item item = requireItem(itemId);

            if (startTime == null || endTime == null) {
                throw new IllegalArgumentException("Start time and end time must not be null");
            }

            if (!endTime.isAfter(startTime)) {
                throw new IllegalArgumentException("Auction end time must be after start time");
            }

            String auctionId = "AUC-" + itemId;
            LocalDateTime now = LocalDateTime.now();
            AuctionStatus initialStatus = startTime.isAfter(now)
                    ? AuctionStatus.OPEN
                    : AuctionStatus.RUNNING;

            try (Connection conn = Database.getInstance().getConnection()) {
                conn.setAutoCommit(false);

                try {
                    boolean itemUpdated = itemDAO.updateAuctionInfo(
                            conn,
                            itemId,
                            initialStatus,
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
                                initialStatus
                        );
                    } else {
                        auctionSaved = auctionDAO.insertAuction(
                                conn,
                                auctionId,
                                itemId,
                                startTime,
                                endTime,
                                initialStatus
                        );
                    }

                    if (!auctionSaved) {
                        throw new SQLException("Cannot save auction");
                    }

                    conn.commit();

                    //update RAM sau khi thành công
                    item.setStartTime(startTime);
                    item.setEndTime(endTime);
                    item.setStatus(initialStatus);
                    item.setAuctionApproved(true);

                    Auction auction = auctionsById.get(itemId);
                    if (auction == null) {
                        auction = new Auction(auctionId, item);
                        auctionsById.put(itemId, auction);
                    }
                    auction.setStatus(initialStatus);

                } catch (SQLException e) {
                    conn.rollback();
                    throw new IllegalStateException("Cannot start auction: " + e.getMessage(), e);
                } finally {
                    conn.setAutoCommit(true);
                }

            } catch (SQLException e) {
                throw new IllegalStateException("Database error when starting auction", e);
            }

        } finally {
            lock.unlock();
        }

    }

    public void relistAuction(String itemId, Seller seller, LocalDateTime startTime, LocalDateTime endTime) {
        validateSeller(seller);
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("Item id must not be empty");
        }
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start time and end time must not be null");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Auction end time must be after start time");
        }
        if (!endTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Auction end time must be in the future");
        }

        ReentrantLock lock = lockForItem(itemId);
        lock.lock();

        try {
            Item item = requireItem(itemId);
            if (!Objects.equals(seller.getId(), item.getSellerId())) {
                throw new IllegalArgumentException("Seller can only relist their own item");
            }
            if (item.getStatus() != AuctionStatus.CANCELED) {
                throw new IllegalStateException("Only CANCELED auctions can be relisted");
            }

            String auctionId = "AUC-" + itemId;
            AuctionStatus initialStatus = startTime.isAfter(LocalDateTime.now())
                    ? AuctionStatus.OPEN
                    : AuctionStatus.RUNNING;

            try (Connection conn = Database.getInstance().getConnection()) {
                conn.setAutoCommit(false);

                try {
                    boolean itemUpdated = itemDAO.relistAuction(
                            conn,
                            itemId,
                            initialStatus,
                            startTime,
                            endTime
                    );
                    if (!itemUpdated) {
                        throw new SQLException("Cannot relist item");
                    }

                    boolean auctionSaved = auctionDAO.existsByItemId(conn, itemId)
                            ? auctionDAO.relistAuction(conn, itemId, startTime, endTime, initialStatus)
                            : auctionDAO.insertAuction(conn, auctionId, itemId, startTime, endTime, initialStatus);
                    if (!auctionSaved) {
                        throw new SQLException("Cannot save relisted auction");
                    }

                    conn.commit();

                    item.setStatus(initialStatus);
                    item.setAuctionApproved(true);
                    item.setStartTime(startTime);
                    item.setEndTime(endTime);
                    item.setCurrentPrice(item.getStartPrice());
                    item.setHighestBidderId(null);
                    item.setHighestBidderUsername(null);
                    autoBidDAO.deactivateByItemId(itemId);

                    Auction auction = auctionsById.get(itemId);
                    if (auction == null) {
                        auction = new Auction(auctionId, item);
                        auctionsById.put(itemId, auction);
                    }
                    auction.setStatus(initialStatus);

                } catch (SQLException e) {
                    conn.rollback();
                    throw new IllegalStateException("Cannot relist auction: " + e.getMessage(), e);
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                throw new IllegalStateException("Database error when relisting auction", e);
            }
        } finally {
            lock.unlock();
        }
    }

    public void requestAuctionApproval(String itemId, Seller seller, LocalDateTime startTime, LocalDateTime endTime) {
        validateSeller(seller);
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("Item id must not be empty");
        }
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start time and end time must not be null");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Auction end time must be after start time");
        }

        ReentrantLock lock = lockForItem(itemId);
        lock.lock();

        try {
            Item item = requireItem(itemId);
            if (!Objects.equals(seller.getId(), item.getSellerId())) {
                throw new IllegalArgumentException("Seller can only request approval for their own item");
            }
            if (item.getStatus() == AuctionStatus.RUNNING) {
                throw new IllegalStateException("Cannot request approval while auction is running");
            }

            try (Connection conn = Database.getInstance().getConnection()) {
                boolean updated = itemDAO.requestAuctionApproval(conn, itemId, startTime, endTime);
                if (!updated) {
                    throw new IllegalStateException("Cannot save auction approval request");
                }
            } catch (SQLException exception) {
                throw new IllegalStateException("Database error when requesting auction approval", exception);
            }

            item.setStatus(AuctionStatus.OPEN);
            item.setAuctionApproved(false);
            item.setStartTime(startTime);
            item.setEndTime(endTime);
        } finally {
            lock.unlock();
        }
    }

    private void finishAuctionLocked(String itemId) {
        Item item = requireItem(itemId);

        String winnerId = item.getHighestBidderId();
        boolean sold = winnerId != null && !winnerId.isBlank();
        double finalPrice = sold ? item.getCurrentPrice() : 0;

        try (Connection conn = Database.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean itemUpdated = itemDAO.updateStatus(conn, itemId, AuctionStatus.FINISHED);
                if (!itemUpdated) throw new SQLException("Cannot update item after finishing auction");

                boolean auctionUpdated = sold
                        ? auctionDAO.finishAuction(conn, itemId, winnerId, finalPrice)
                        : auctionDAO.updateStatusAndClearWinner(conn, itemId, AuctionStatus.FINISHED);
                if (!auctionUpdated) throw new SQLException("Cannot update auction status to FINISHED");

                conn.commit();

                item.setStatus(AuctionStatus.FINISHED);
                autoBidDAO.deactivateByItemId(itemId);

                Auction auction = auctionsById.get(itemId);
                if (auction != null) auction.finishAuction();

                if (auctionSubject != null) auctionSubject.notifyObservers(item, "AUCTION_FINISHED");
            } catch (SQLException e) {
                conn.rollback();
                throw new IllegalStateException("Cannot finish auction: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database error when finishing auction", e);
        }
    }

    public void finishAuction(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("Item id must not be empty");
        }

        ReentrantLock lock = lockForItem(itemId);
        lock.lock();

        try {
            finishAuctionLocked(itemId);
        } finally {
            lock.unlock();
        }
    }

    public void activateScheduledAuctions() {
        List<String> readyItemIds;
        try {
            readyItemIds = auctionDAO.findScheduledItemIdsReadyToStart();
        } catch (SQLException e) {
            LOGGER.error("Cannot query scheduled auctions: {}", e.getMessage());
            return;
        }
        for (String itemId : readyItemIds) {
            ReentrantLock lock = lockForItem(itemId);
            lock.lock();
            try {
                Item item = requireItem(itemId);
                if (item.getStatus() != AuctionStatus.OPEN || !item.isAuctionApproved()) continue;
                if (item.getStartTime() == null || item.getStartTime().isAfter(LocalDateTime.now())) continue;

                try (Connection conn = Database.getInstance().getConnection()) {
                    conn.setAutoCommit(false);
                    try {
                        itemDAO.updateStatus(conn, itemId, AuctionStatus.RUNNING);
                        auctionDAO.updateStatus(conn, itemId, AuctionStatus.RUNNING);
                        conn.commit();
                        item.setStatus(AuctionStatus.RUNNING);
                        Auction auction = auctionsById.get(itemId);
                        if (auction != null) auction.setStatus(AuctionStatus.RUNNING);
                        if (auctionSubject != null) auctionSubject.notifyObservers(item, "AUCTION_STARTED");
                    } catch (SQLException e) {
                        conn.rollback();
                        LOGGER.warn("Cannot activate scheduled auction {}: {}", itemId, e.getMessage());
                    } finally {
                        conn.setAutoCommit(true);
                    }
                } catch (SQLException e) {
                    LOGGER.error("DB error activating auction {}", itemId, e);
                }
            } catch (Exception e) {
                LOGGER.warn("Activate scheduled failed for {}: {}", itemId, e.getMessage());
            } finally {
                lock.unlock();
            }
        }
    }

    public void markAsPaid(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("Item id must not be empty");
        }

        ReentrantLock lock = lockForItem(itemId);
        lock.lock();

        try {
            Item item = requireItem(itemId);

            if (item.getStatus() != AuctionStatus.FINISHED) {
                throw new IllegalStateException("Only FINISHED auctions can be marked as PAID");
            }

            try (Connection conn = Database.getInstance().getConnection()) {
                conn.setAutoCommit(false);

                try {
                    boolean itemUpdated = itemDAO.updateStatus(conn, itemId, AuctionStatus.PAID);
                    if (!itemUpdated) throw new SQLException("Cannot update item status to PAID");

                    boolean auctionUpdated = auctionDAO.updateStatus(conn, itemId, AuctionStatus.PAID);
                    if (!auctionUpdated) throw new SQLException("Cannot update auction status to PAID");

                    // THÊM: trừ tiền bidder khi thanh toán
                    String bidderId = item.getHighestBidderId();
                    double finalPrice = item.getCurrentPrice();
                    if (bidderId != null && !bidderId.isBlank()) {
                        if (!userDAO.deductBidderBalance(conn, bidderId, finalPrice)) {
                            throw new SQLException("Cannot deduct balance from bidder");
                        }
                        if (!userDAO.addSellerBalance(conn, item.getSellerId(), finalPrice)) {
                            throw new SQLException("Cannot add final price to seller balance");
                        }
                    }

                    conn.commit();

                    item.setStatus(AuctionStatus.PAID);

                    // THÊM: cập nhật bidder balance trong RAM
                    if (bidderId != null) {
                        authManager.findById(bidderId).ifPresent(bidder ->
                                bidder.setBalance(bidder.getBalance() - finalPrice)
                        );
                        authManager.findById(item.getSellerId()).ifPresent(seller ->
                                seller.setBalance(seller.getBalance() + finalPrice)
                        );
                    }
                    autoBidDAO.deactivateByItemId(itemId);
                    Auction auction = auctionsById.get(itemId);
                    if (auction != null) auction.setStatus(AuctionStatus.PAID);

                    if (auctionSubject != null) {
                        auctionSubject.notifyObservers(item, "AUCTION_PAID");
                    }

                } catch (SQLException e) {
                    conn.rollback();
                    throw new IllegalStateException("Cannot mark auction as paid: " + e.getMessage(), e);
                } finally {
                    conn.setAutoCommit(true);
                }

            } catch (SQLException e) {
                throw new IllegalStateException("Database error when marking auction as paid", e);
            }

        } finally {
            lock.unlock();
        }
    }

    public void winnerDecline(String itemId, String bidderId) {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("Item id must not be empty");
        }

        ReentrantLock lock = lockForItem(itemId);
        lock.lock();

        try {
            Item item = requireItem(itemId);

            if (item.getStatus() != AuctionStatus.FINISHED) {
                throw new IllegalStateException("Only FINISHED auctions can be declined");
            }

            if (!Objects.equals(bidderId, item.getHighestBidderId())) {
                throw new IllegalArgumentException("Only the auction winner can decline payment");
            }

            try (Connection conn = Database.getInstance().getConnection()) {
                conn.setAutoCommit(false);

                try {
                    boolean itemUpdated = itemDAO.updateStatus(conn, itemId, AuctionStatus.CANCELED);
                    if (!itemUpdated) throw new SQLException("Cannot update item status to CANCELED");

                    boolean auctionUpdated = auctionDAO.updateStatusAndClearWinner(conn, itemId, AuctionStatus.CANCELED);
                    if (!auctionUpdated) throw new SQLException("Cannot update auction status to CANCELED");

                    conn.commit();

                    item.setStatus(AuctionStatus.CANCELED);
                    item.setHighestBidderId(null);
                    item.setHighestBidderUsername(null);
                    autoBidDAO.deactivateByItemId(itemId);

                    Auction auction = auctionsById.get(itemId);
                    if (auction != null) auction.setStatus(AuctionStatus.CANCELED);

                    if (auctionSubject != null) {
                        auctionSubject.notifyObservers(item, "AUCTION_CANCELED");
                    }

                } catch (SQLException e) {
                    conn.rollback();
                    throw new IllegalStateException("Cannot decline win: " + e.getMessage(), e);
                } finally {
                    conn.setAutoCommit(true);
                }

            } catch (SQLException e) {
                throw new IllegalStateException("Database error when declining win", e);
            }

        } finally {
            lock.unlock();
        }
    }

    public void cancelExpiredUnpaidAuctions(int minutes) {
        try {
            for (String itemId : auctionDAO.findUnpaidFinishedItemIdsOlderThan(minutes)) {
                try {
                    Item item = requireItem(itemId);
                    String winnerId = item.getHighestBidderId();
                    if (item.getStatus() == AuctionStatus.FINISHED
                            && winnerId != null
                            && !winnerId.isBlank()) {
                        winnerDecline(itemId, winnerId);
                    }
                } catch (RuntimeException exception) {
                    LOGGER.warn("Cannot cancel unpaid auction {}: {}", itemId, exception.getMessage());
                }
            }
        } catch (SQLException exception) {
            LOGGER.error("Cannot query expired unpaid auctions: {}", exception.getMessage());
        }
    }

    public void cancelAuction(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("Item id must not be empty");
        }

        ReentrantLock lock = lockForItem(itemId);
        lock.lock();

        try {
            Item item = requireItem(itemId);

            AuctionStatus current = item.getStatus();
            if (current == AuctionStatus.FINISHED || current == AuctionStatus.PAID || current == AuctionStatus.CANCELED) {
                throw new IllegalStateException("Cannot cancel an auction with status: " + current);
            }

            try (Connection conn = Database.getInstance().getConnection()) {
                conn.setAutoCommit(false);

                try {
                    boolean itemUpdated = itemDAO.updateStatus(conn, itemId, AuctionStatus.CANCELED);
                    if (!itemUpdated) throw new SQLException("Cannot update item status to CANCELED");

                    boolean auctionUpdated;
                    if (current == AuctionStatus.RUNNING) {
                        // clear winner/price since the auction did not complete normally
                        auctionUpdated = auctionDAO.updateStatusAndClearWinner(conn, itemId, AuctionStatus.CANCELED);
                    } else {
                        auctionUpdated = auctionDAO.updateStatus(conn, itemId, AuctionStatus.CANCELED);
                    }
                    if (!auctionUpdated) throw new SQLException("Cannot update auction status to CANCELED");

                    conn.commit();

                    item.setStatus(AuctionStatus.CANCELED);
                    autoBidDAO.deactivateByItemId(itemId);
                    Auction auction = auctionsById.get(itemId);
                    if (auction != null) auction.setStatus(AuctionStatus.CANCELED);

                    if (auctionSubject != null) {
                        auctionSubject.notifyObservers(item, "AUCTION_CANCELED");
                    }

                } catch (SQLException e) {
                    conn.rollback();
                    throw new IllegalStateException("Cannot cancel auction: " + e.getMessage(), e);
                } finally {
                    conn.setAutoCommit(true);
                }

            } catch (SQLException e) {
                throw new IllegalStateException("Database error when canceling auction", e);
            }

        } finally {
            lock.unlock();
        }
    }

    public Bid placeBid(String itemId, Bidder bidder, double bidAmount) {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("Item id must not be empty");
        }

        ReentrantLock lock = lockForItem(itemId);
        lock.lock();

        try {
            if (bidder == null) {
                throw new IllegalArgumentException("Bidder must not be null");
            }

            Item item = requireItem(itemId);

            if (item.getStatus() != AuctionStatus.RUNNING) {
                throw new IllegalStateException("Auction is not running");
            }

            if (Objects.equals(item.getSellerId(), bidder.getId())) {
                throw new IllegalArgumentException("Seller cannot bid on their own item");
            }

            if (item.getEndTime() != null && LocalDateTime.now().isAfter(item.getEndTime())) {
                finishAuctionLocked(itemId);
                throw new IllegalStateException("Auction already finished");
            }

            if (bidAmount <= item.getCurrentPrice()) {
                throw new IllegalArgumentException("Bid amount must be greater than current price");
            }

            String bidId = "Bid-" + UUID.randomUUID();
            LocalDateTime bidTime = LocalDateTime.now();

            try (Connection conn = Database.getInstance().getConnection()) {
                conn.setAutoCommit(false);

                try {
                    String auctionId = auctionDAO.findAuctionByItemId(conn, itemId);

                    if (auctionId == null) {
                        throw new SQLException("Cannot find auction for item: " + itemId);
                    }

                    boolean itemUpdated = itemDAO.updateAfterBid(
                            conn,
                            itemId,
                            Money.toDatabaseAmount(bidAmount),
                            bidder.getId()
                    );

                    if (!itemUpdated) {
                        throw new IllegalStateException(
                                "Bid failed because another bidder placed a higher or equal bid first, or auction is no longer running."
                        );
                    }

                    boolean bidSaved = bidDAO.insertBid(
                            conn,
                            bidId,
                            auctionId,
                            itemId,
                            bidder.getId(),
                            bidAmount,
                            bidTime
                    );

                    if (!bidSaved) {
                        throw new SQLException("Cannot insert bid");
                    }

                    extendAuctionIfNearEnd(conn, item, bidTime);

                    conn.commit();

                } catch (SQLException e) {
                    conn.rollback();
                    throw new IllegalStateException("Cannot place bid: " + e.getMessage(), e);
                } catch (RuntimeException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }

            } catch (SQLException e) {
                throw new IllegalStateException("Database error when placing bid", e);
            }

            Bid bid = new Bid(bidId, bidder, bidAmount, bidTime);

            item.setCurrentPrice(bidAmount);
            item.setHighestBidderId(bidder.getId());
            item.setHighestBidderUsername(bidder.getUserName());
            item.addBid(bid);

            //kiểm tra autobid
            processAutoBidsLocked(itemId);

            return bid;

        } finally {
            lock.unlock();
        }
    }

    public Item setAutoBid(String itemId, Bidder bidder, double maxBid, double incrementAmount) {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("Item id must not be empty");
        }

        if (bidder == null) {
            throw new IllegalArgumentException("Bidder must not be null");
        }

        ReentrantLock lock = lockForItem(itemId);
        lock.lock();

        try {
            Item item = requireItem(itemId);

            if (item.getStatus() != AuctionStatus.RUNNING) {
                throw new IllegalStateException("Auction is not running");
            }

            if (Objects.equals(item.getSellerId(), bidder.getId())) {
                throw new IllegalArgumentException("Seller cannot enable auto-bid on their own item");
            }

            if (item.getEndTime() != null && LocalDateTime.now().isAfter(item.getEndTime())) {
                finishAuctionLocked(itemId);
                throw new IllegalStateException("Auction already finished");
            }

            if (maxBid <= item.getCurrentPrice()) {
                throw new IllegalArgumentException("Max bid must be greater than current price");
            }

            if (incrementAmount <= 0) {
                throw new IllegalArgumentException("Increment amount must be greater than 0");
            }

            Bidder latestBidder = authManager.findById(bidder.getId())
                    .filter(Bidder.class::isInstance)
                    .map(Bidder.class::cast)
                    .orElse(bidder);
            if (latestBidder.getBalance() < maxBid) {
                throw new IllegalArgumentException("Bidder balance is not enough for this auto-bid max amount");
            }

            String autoBidId = "AUTO-BID-" + UUID.randomUUID();

            AutoBid autoBid = new AutoBid(
                    autoBidId,
                    itemId,
                    latestBidder.getId(),
                    latestBidder.getUserName(),
                    maxBid,
                    incrementAmount
            );

            boolean saved = autoBidDAO.upsertAutoBid(autoBid);
            if (!saved) {
                throw new IllegalStateException("Cannot save auto-bid");
            }

            processAutoBidsLocked(itemId);

            return item;
        } finally {
            lock.unlock();
        }
    }

    public Item cancelAutoBid(String itemId, Bidder bidder) {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("Item id must not be empty");
        }

        if (bidder == null) {
            throw new IllegalArgumentException("Bidder must not be null");
        }

        ReentrantLock lock = lockForItem(itemId);
        lock.lock();

        try {
            Item item = requireItem(itemId);

            boolean canceled = autoBidDAO.deactivate(itemId, bidder.getId());
            if (!canceled) {
                throw new IllegalStateException("No active auto-bid found for this item");
            }

            return item;
        } finally {
            lock.unlock();
        }
    }

    private void processAutoBidsLocked(String itemId) {
        Item item = requireItem(itemId);

        autoBidDAO.deactivateExhausted(itemId, item.getCurrentPrice());

        List<AutoBid> candidates = autoBidDAO.findActiveByItemId(itemId).stream()
                .filter(autoBid -> autoBid.getBidderId() != null)
                .filter(autoBid -> !autoBid.getBidderId().equals(item.getSellerId()))
                .filter(autoBid -> autoBid.getMaxBid() > item.getCurrentPrice())
                .filter(autoBid -> autoBid.getIncrementAmount() > 0)
                .sorted(this::compareAutoBidPriority)
                .toList();

        if (candidates.isEmpty()) {
            return;
        }

        AutoBid winner = candidates.get(0);
        AutoBid second = candidates.size() > 1 ? candidates.get(1) : null;
        if (winner.getBidderId().equals(item.getHighestBidderId()) && second == null) {
            return;
        }

        double minimumIncrement = winner.getIncrementAmount();
        double targetAmount = item.getCurrentPrice() + minimumIncrement;
        if (second != null) {
            targetAmount = Math.max(targetAmount, second.getMaxBid() + minimumIncrement);
        }
        double nextAmount = Math.min(winner.getMaxBid(), targetAmount);

        if (nextAmount <= item.getCurrentPrice()) {
            autoBidDAO.deactivate(winner.getItemId(), winner.getBidderId());
            return;
        }

        User user = authManager.findById(winner.getBidderId())
                .orElseThrow(() -> new IllegalStateException("Auto-bid Bidder cannot exist"));

        if (!(user instanceof Bidder autoBidder)) {
            autoBidDAO.deactivate(winner.getItemId(), winner.getBidderId());
            throw new IllegalStateException("Auto-bid user is not a bidder");
        }

        if (autoBidder.getBalance() < nextAmount) {
            autoBidDAO.deactivate(winner.getItemId(), winner.getBidderId());
            return;
        }

        placeAutoBidLocked(itemId, item, autoBidder, nextAmount);
        autoBidDAO.deactivateExhausted(itemId, item.getCurrentPrice());

        if (auctionSubject != null) {
            auctionSubject.notifyObservers(item, "BID_PLACED");
        }
    }

    private int compareAutoBidPriority(AutoBid a, AutoBid b) {
        int byMaxBid = Double.compare(b.getMaxBid(), a.getMaxBid());
        if (byMaxBid != 0) {
            return byMaxBid;
        }

        LocalDateTime aTime = a.getCreatedAt();
        LocalDateTime bTime = b.getCreatedAt();
        if (aTime == null && bTime == null) {
            return 0;
        }
        if (aTime == null) {
            return 1;
        }
        if (bTime == null) {
            return -1;
        }
        return aTime.compareTo(bTime);
    }

    private void placeAutoBidLocked(String itemId, Item item, Bidder bidder, double bidAmount) {
        String bidId = "AUTO-BID-TRANSACTION-" + UUID.randomUUID();
        LocalDateTime bidTime = LocalDateTime.now();

        try (Connection conn = Database.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            try {
                String auctionId = auctionDAO.findAuctionByItemId(conn, itemId);

                if (auctionId == null) {
                    throw new SQLException("Cannot find auction for item: " + itemId);
                }

                boolean itemUpdated = itemDAO.updateAfterBid(
                        conn,
                        itemId,
                        Money.toDatabaseAmount(bidAmount),
                        bidder.getId()
                );

                if (!itemUpdated) {
                    throw new IllegalStateException("Auto-bid failed because another bidder placed a higher or equal bid first");
                }

                boolean bidSaved = bidDAO.insertBid(
                        conn,
                        bidId,
                        auctionId,
                        itemId,
                        bidder.getId(),
                        bidAmount,
                        bidTime
                );

                if (!bidSaved) {
                    throw new SQLException("Cannot insert auto-bid");
                }

                extendAuctionIfNearEnd(conn, item, bidTime);

                conn.commit();
            } catch (SQLException exception) {
                conn.rollback();
                throw new IllegalStateException("Cannot place auto-bid: " + exception.getMessage(), exception);
            } catch (RuntimeException exception) {
                conn.rollback();
                throw exception;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Database error when placing auto-bid", exception);
        }

        Bid bid = new Bid(bidId, bidder, bidAmount, bidTime);

        item.setCurrentPrice(bidAmount);
        item.setHighestBidderId(bidder.getId());
        item.setHighestBidderUsername(bidder.getUserName());

        item.addBid(bid);
    }

    private void extendAuctionIfNearEnd(Connection conn, Item item, LocalDateTime bidTime) throws SQLException {
        LocalDateTime endTime = item.getEndTime();
        if (endTime == null || !endTime.isAfter(bidTime)) {
            return;
        }

        long secondsLeft = Duration.between(bidTime, endTime).getSeconds();
        if (secondsLeft > ANTI_SNIPING_THRESHOLD_SECONDS) {
            return;
        }

        LocalDateTime extendedEndTime = bidTime.plusSeconds(ANTI_SNIPING_EXTENSION_SECONDS);
        if (!extendedEndTime.isAfter(endTime)) {
            return;
        }

        // Ghi chu: anti-sniping giu phien con toi thieu 60 giay sau bid cuoi,
        // cap nhat ca items va auctions trong cung transaction de tranh lech du lieu.
        boolean itemUpdated = itemDAO.updateEndTime(conn, item.getId(), extendedEndTime);
        if (!itemUpdated) {
            throw new SQLException("Cannot extend item end time");
        }

        boolean auctionUpdated = auctionDAO.updateEndTime(conn, item.getId(), extendedEndTime);
        if (!auctionUpdated) {
            throw new SQLException("Cannot extend auction end time");
        }

        item.setEndTime(extendedEndTime);
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

    public AutoBid findActiveAutoBid(String itemId, String bidderId) {
        if (itemId == null || itemId.isBlank() || bidderId == null || bidderId.isBlank()) {
            return null;
        }
        return autoBidDAO.findActiveByItemAndBidder(itemId, bidderId);
    }

    synchronized void resetForTest() {
        auctionsById.clear();
        itemLocks.clear();
        itemManager.resetForTest();
        authManager.resetForTest();
    }

    // tìm khóa theo itemId nếu có rồi thì trả về khóa đó,
    // nếu chưa có thì tạo new ReentrantLock, lưu vào map và trả về khóa mới tạo
    private ReentrantLock lockForItem(String itemId) {
        return itemLocks.computeIfAbsent(itemId, id -> new ReentrantLock());
    }


    private void validateSeller(Seller seller) {
        if (seller == null) {
            throw new IllegalArgumentException("Seller must not be null");
        }

        Optional<User> existingSeller = authManager.findById(seller.getId());
        if (existingSeller.isEmpty() || !(existingSeller.get() instanceof Seller)) {
            throw new IllegalArgumentException("Seller account is not registered");
        }
        if (!existingSeller.get().isApproved()) {
            throw new IllegalStateException("Seller account is awaiting admin approval");
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

    public ItemManager getItemManager() {
        return this.itemManager; // itemManager là field trong AuctionManager
    }

    public AuthManager getAuthManager() {
        return this.authManager; // authManager là field trong AuctionManager
    }
}
