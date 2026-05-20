package com.auction.system.server.manager;

import com.auction.system.model.auction.AuctionStatus;
import com.auction.system.model.item.Item;
import com.auction.system.model.user.Admin;
import com.auction.system.model.user.Seller;
import com.auction.system.model.user.User;
import com.auction.system.server.dao.ItemDAO;
import com.auction.system.server.dao.UserDAO;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public class AdminManager {
    private static final AdminManager INSTANCE = new AdminManager();

    private final AuthManager authManager = AuthManager.getInstance();
    private final AuctionManager auctionManager = AuctionManager.getInstance();
    private final UserDAO userDAO = new UserDAO();
    private final ItemDAO itemDAO = new ItemDAO();

    private AdminManager() {
    }

    public static AdminManager getInstance() {
        return INSTANCE;
    }

    public synchronized List<User> getPendingSellers(User adminUser) {
        requireAdmin(adminUser);
        return authManager.getAllUsers().stream()
                .filter(Seller.class::isInstance)
                .filter(user -> !user.isApproved())
                .sorted(Comparator.comparing(User::getId, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    public synchronized List<Item> getPendingAuctionRequests(User adminUser) {
        requireAdmin(adminUser);
        return auctionManager.getAllItems().stream()
                .filter(this::hasPendingAuctionRequest)
                .sorted(Comparator
                        .comparing(Item::getStartTime, Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(Item::getId, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    public synchronized User updateSellerApproval(User adminUser, String sellerId, boolean approved) {
        requireAdmin(adminUser);
        User seller = authManager.findById(sellerId)
                .filter(Seller.class::isInstance)
                .orElseThrow(() -> new IllegalArgumentException("Seller does not exist"));

        if (!userDAO.updateApproval(sellerId, approved)) {
            throw new IllegalStateException("Cannot update seller approval");
        }

        seller.setApproved(approved);
        return seller;
    }

    public synchronized Item updateAuctionApproval(User adminUser, String itemId, boolean approved) {
        requireAdmin(adminUser);
        Item item = auctionManager.findItemById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item does not exist"));
        if (!hasPendingAuctionRequest(item)) {
            throw new IllegalStateException("Auction does not have a pending approval request");
        }

        if (approved) {
            if (item.getEndTime() != null && !item.getEndTime().isAfter(LocalDateTime.now())) {
                throw new IllegalStateException("Auction request has already expired");
            }
            auctionManager.startAuction(itemId, item.getStartTime(), item.getEndTime());
            return auctionManager.findItemById(itemId).orElse(item);
        }

        if (!itemDAO.clearAuctionRequest(itemId)) {
            throw new IllegalStateException("Cannot reject auction request");
        }

        item.setStatus(AuctionStatus.OPEN);
        item.setAuctionApproved(false);
        item.setStartTime(null);
        item.setEndTime(null);
        return item;
    }

    private boolean hasPendingAuctionRequest(Item item) {
        return item != null
                && !item.isAuctionApproved()
                && item.getStatus() == AuctionStatus.OPEN
                && item.getStartTime() != null
                && item.getEndTime() != null;
    }

    private void requireAdmin(User adminUser) {
        if (!(adminUser instanceof Admin)) {
            throw new IllegalArgumentException("Only admin accounts can perform this action");
        }
    }
}
