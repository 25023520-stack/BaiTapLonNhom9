package com.auction.system.server.manager;

import com.auction.system.model.auction.AuctionStatus;
import com.auction.system.model.item.Item;
import com.auction.system.model.user.Admin;
import com.auction.system.model.user.Bidder;
import com.auction.system.model.user.DepositRequest;
import com.auction.system.model.user.Seller;
import com.auction.system.model.user.User;
import com.auction.system.server.database.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AdminManagerTest {

    private static final String TEST_PREFIX = "TEST-ADMIN-";

    private AdminManager adminManager;
    private AuctionManager auctionManager;
    private Admin admin;
    private Seller seller;
    private Bidder bidder;

    @BeforeEach
    void setUp() {
        Database.getInstance().initializeDatabase();
        cleanUpTestData();

        adminManager = AdminManager.getInstance();
        auctionManager = AuctionManager.getInstance();
        auctionManager.resetForTest();

        String token = shortToken();
        admin = new Admin(
                TEST_PREFIX + "ADMIN-" + token,
                "Admin User",
                "admin_" + token,
                "admin_" + token + "@admintest.local",
                "secret"
        );
        seller = new Seller(
                TEST_PREFIX + "SELLER-" + token,
                "Test Seller",
                "seller_adm_" + token,
                "seller_adm_" + token + "@admintest.local",
                "secret"
        );
        bidder = new Bidder(
                TEST_PREFIX + "BIDDER-" + token,
                "Test Bidder",
                "bidder_adm_" + token,
                "bidder_adm_" + token + "@admintest.local",
                "secret"
        );

        auctionManager.registerUser(admin);
        auctionManager.registerUser(seller);
        auctionManager.registerUser(bidder);
    }

    @AfterEach
    void tearDown() {
        if (auctionManager != null) {
            auctionManager.resetForTest();
        }
        cleanUpTestData();
    }

    // ---- requireAdmin enforcement ----

    @Test
    void getPendingSellersThrowsWhenCalledByNonAdmin() {
        assertThrows(IllegalArgumentException.class,
                () -> adminManager.getPendingSellers(seller));
        assertThrows(IllegalArgumentException.class,
                () -> adminManager.getPendingSellers(bidder));
    }

    @Test
    void getPendingAuctionRequestsThrowsWhenCalledByNonAdmin() {
        assertThrows(IllegalArgumentException.class,
                () -> adminManager.getPendingAuctionRequests(seller));
    }

    @Test
    void getPendingDepositRequestsThrowsWhenCalledByNonAdmin() {
        assertThrows(IllegalArgumentException.class,
                () -> adminManager.getPendingDepositRequests(bidder));
    }

    // ---- getPendingSellers ----

    @Test
    void getPendingSellersReturnsUnapprovedSeller() {
        seller.setApproved(false);
        // Update in DB
        adminManager.updateSellerApproval(admin, seller.getId(), false);

        List<User> pending = adminManager.getPendingSellers(admin);
        assertTrue(pending.stream().anyMatch(u -> u.getId().equals(seller.getId())));
    }

    @Test
    void getPendingSellersDoesNotReturnApprovedSeller() {
        adminManager.updateSellerApproval(admin, seller.getId(), true);

        List<User> pending = adminManager.getPendingSellers(admin);
        assertTrue(pending.stream().noneMatch(u -> u.getId().equals(seller.getId())));
    }

    // ---- updateSellerApproval ----

    @Test
    void updateSellerApprovalApprovesSeller() {
        adminManager.updateSellerApproval(admin, seller.getId(), false);
        User updated = adminManager.updateSellerApproval(admin, seller.getId(), true);
        assertTrue(updated.isApproved());
    }

    @Test
    void updateSellerApprovalRejectsSeller() {
        User updated = adminManager.updateSellerApproval(admin, seller.getId(), false);
        assertFalse(updated.isApproved());
    }

    @Test
    void updateSellerApprovalThrowsWhenSellerDoesNotExist() {
        assertThrows(IllegalArgumentException.class,
                () -> adminManager.updateSellerApproval(admin, "NONEXISTENT-SELLER-ID", true));
    }

    @Test
    void updateSellerApprovalThrowsWhenTargetIsBidder() {
        assertThrows(IllegalArgumentException.class,
                () -> adminManager.updateSellerApproval(admin, bidder.getId(), true));
    }

    // ---- createDepositRequest ----

    @Test
    void createDepositRequestSucceedsForBidder() {
        DepositRequest request = adminManager.createDepositRequest(bidder, 500.0);
        assertNotNull(request);
        assertNotNull(request.getId());
        assertEquals(bidder.getId(), request.getBidderId());
        assertEquals(500.0, request.getAmount(), 0.001);
        assertEquals("PENDING", request.getStatus());
        assertNotNull(request.getCreatedAt());
    }

    @Test
    void createDepositRequestThrowsForNonBidder() {
        assertThrows(IllegalArgumentException.class,
                () -> adminManager.createDepositRequest(seller, 500.0));
        assertThrows(IllegalArgumentException.class,
                () -> adminManager.createDepositRequest(admin, 500.0));
    }

    @Test
    void createDepositRequestThrowsForZeroOrNegativeAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> adminManager.createDepositRequest(bidder, 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> adminManager.createDepositRequest(bidder, -100.0));
    }

    // ---- approveDepositRequest ----

    @Test
    void approveDepositRequestIncreasesBalance() {
        double depositAmount = 1000.0;
        DepositRequest request = adminManager.createDepositRequest(bidder, depositAmount);

        User updatedBidder = adminManager.approveDepositRequest(admin, request.getId());
        assertNotNull(updatedBidder);
        assertEquals(bidder.getId(), updatedBidder.getId());
    }

    @Test
    void approveDepositRequestThrowsWhenCalledByNonAdmin() {
        DepositRequest request = adminManager.createDepositRequest(bidder, 500.0);
        assertThrows(IllegalArgumentException.class,
                () -> adminManager.approveDepositRequest(seller, request.getId()));
    }

    @Test
    void approveDepositRequestThrowsOnBlankId() {
        assertThrows(IllegalArgumentException.class,
                () -> adminManager.approveDepositRequest(admin, ""));
        assertThrows(IllegalArgumentException.class,
                () -> adminManager.approveDepositRequest(admin, null));
    }

    @Test
    void approveDepositRequestThrowsWhenAlreadyApproved() {
        DepositRequest request = adminManager.createDepositRequest(bidder, 200.0);
        adminManager.approveDepositRequest(admin, request.getId());

        assertThrows(IllegalStateException.class,
                () -> adminManager.approveDepositRequest(admin, request.getId()));
    }

    @Test
    void approveDepositRequestThrowsWhenRequestDoesNotExist() {
        assertThrows(IllegalArgumentException.class,
                () -> adminManager.approveDepositRequest(admin, "DEP-NONEXISTENT-" + shortToken()));
    }

    // ---- getPendingAuctionRequests ----

    @Test
    void getPendingAuctionRequestsReturnsItemWithPendingRequest() {
        String itemId = TEST_PREFIX + "ITEM-" + shortToken();
        Item item = new Item(itemId, "Test Item", "Desc", 500.0, 500.0, AuctionStatus.OPEN);
        auctionManager.addItem(item, seller);

        LocalDateTime start = LocalDateTime.now().plusMinutes(5);
        LocalDateTime end = start.plusHours(1);
        auctionManager.requestAuctionApproval(itemId, seller, start, end);

        List<Item> pending = adminManager.getPendingAuctionRequests(admin);
        assertTrue(pending.stream().anyMatch(i -> i.getId().equals(itemId)));
    }

    @Test
    void getPendingDepositRequestsReturnsPendingRequests() {
        adminManager.createDepositRequest(bidder, 300.0);
        List<DepositRequest> pending = adminManager.getPendingDepositRequests(admin);
        assertNotNull(pending);
        assertTrue(pending.stream().anyMatch(r -> r.getBidderId().equals(bidder.getId())));
    }

    // ---- updateAuctionApproval ----

    @Test
    void updateAuctionApprovalThrowsWhenItemHasNoPendingRequest() {
        String itemId = TEST_PREFIX + "ITEM-NOREQ-" + shortToken();
        Item item = new Item(itemId, "No Req Item", "Desc", 500.0, 500.0, AuctionStatus.OPEN);
        auctionManager.addItem(item, seller);

        assertThrows(IllegalStateException.class,
                () -> adminManager.updateAuctionApproval(admin, itemId, true));
    }

    @Test
    void updateAuctionApprovalThrowsWhenItemDoesNotExist() {
        assertThrows(IllegalArgumentException.class,
                () -> adminManager.updateAuctionApproval(admin, "NONEXISTENT-ITEM", true));
    }

    @Test
    void updateAuctionApprovalRejectRequestClearsAuctionFields() {
        String itemId = TEST_PREFIX + "ITEM-REJ-" + shortToken();
        Item item = new Item(itemId, "Reject Item", "Desc", 500.0, 500.0, AuctionStatus.OPEN);
        auctionManager.addItem(item, seller);

        LocalDateTime start = LocalDateTime.now().plusMinutes(5);
        LocalDateTime end = start.plusHours(1);
        auctionManager.requestAuctionApproval(itemId, seller, start, end);

        Item result = adminManager.updateAuctionApproval(admin, itemId, false);
        assertNotNull(result);
        assertEquals(AuctionStatus.OPEN, result.getStatus());
        assertFalse(result.isAuctionApproved());
        assertNull(result.getStartTime());
        assertNull(result.getEndTime());
    }

    private void cleanUpTestData() {
        String[] statements = {
            "DELETE FROM deposit_requests WHERE bidder_id LIKE 'TEST-ADMIN-%'",
            "DELETE FROM auto_bids WHERE item_id LIKE 'TEST-ADMIN-%' OR bidder_id LIKE 'TEST-ADMIN-%'",
            "DELETE FROM bids WHERE item_id LIKE 'TEST-ADMIN-%' OR bidder_id LIKE 'TEST-ADMIN-%'",
            "DELETE FROM auctions WHERE item_id LIKE 'TEST-ADMIN-%'",
            "DELETE FROM items WHERE id LIKE 'TEST-ADMIN-%' OR seller_id LIKE 'TEST-ADMIN-%'",
            "DELETE FROM users WHERE id LIKE 'TEST-ADMIN-%'"
        };

        try (Connection conn = Database.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (String sql : statements) {
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Cannot clean admin test data", e);
        }
    }

    private String shortToken() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
