package com.auction.system.server.manager;

import com.auction.system.model.auction.AuctionStatus;
import com.auction.system.model.item.Item;
import com.auction.system.model.user.Admin;
import com.auction.system.model.user.Bidder;
import com.auction.system.model.payment.DepositRequest;
import com.auction.system.model.user.Seller;
import com.auction.system.model.user.User;
import com.auction.system.server.dao.DepositRequestDAO;
import com.auction.system.server.dao.ItemDAO;
import com.auction.system.server.dao.UserDAO;
import com.auction.system.server.database.Database;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class AdminManager {
    private static final AdminManager INSTANCE = new AdminManager();

    private final AuthManager authManager = AuthManager.getInstance();
    private final AuctionManager auctionManager = AuctionManager.getInstance();
    private final UserDAO userDAO = new UserDAO();
    private final ItemDAO itemDAO = new ItemDAO();
    private final DepositRequestDAO depositRequestDAO = new DepositRequestDAO();

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

    public synchronized List<DepositRequest> getPendingDepositRequests(User adminUser) {
        requireAdmin(adminUser);
        return depositRequestDAO.findPending();
    }

    public synchronized DepositRequest createDepositRequest(User user, double amount) {
        if (!(user instanceof Bidder bidder)) {
            throw new IllegalArgumentException("Only bidder accounts can request deposits");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than 0");
        }

        // Bidder chi tao yeu cau nap tien; tien chi duoc cong sau khi admin duyet.
        DepositRequest request = new DepositRequest(
                "DEP-" + UUID.randomUUID(),
                bidder.getId(),
                bidder.getFullName(),
                amount,
                "PENDING",
                LocalDateTime.now(),
                null,
                null
        );
        if (!depositRequestDAO.insert(request)) {
            throw new IllegalStateException("Cannot create deposit request");
        }
        return request;
    }

    public synchronized User approveDepositRequest(User adminUser, String requestId) {
        requireAdmin(adminUser);
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("Deposit request id is required");
        }

        // Duyet nap tien phai la transaction: khoa request, cong so du, roi danh dau da duyet.
        // Neu mot buoc loi, rollback de tranh trang thai request da duyet nhung tien chua cong.
        try (Connection conn = Database.getInstance().getConnection()) {
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                DepositRequest request = depositRequestDAO.findByIdForUpdate(conn, requestId);
                if (request == null) {
                    throw new IllegalArgumentException("Deposit request does not exist");
                }
                if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
                    throw new IllegalStateException("Deposit request has already been reviewed");
                }
                if (!userDAO.addBalance(conn, request.getBidderId(), request.getAmount())) {
                    throw new IllegalStateException("Cannot update bidder balance");
                }
                if (!depositRequestDAO.markApproved(conn, requestId, adminUser.getId())) {
                    throw new IllegalStateException("Cannot approve deposit request");
                }

                conn.commit();
                conn.setAutoCommit(oldAutoCommit);
                return authManager.findById(request.getBidderId())
                        .orElseThrow(() -> new IllegalStateException("Bidder does not exist after deposit"));
            } catch (RuntimeException | SQLException exception) {
                conn.rollback();
                conn.setAutoCommit(oldAutoCommit);
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot approve deposit request", exception);
        }
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
