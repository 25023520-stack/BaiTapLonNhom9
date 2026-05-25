package com.auction.system.server.manager;

import com.auction.system.model.auction.AuctionStatus;
import com.auction.system.model.item.Item;
import com.auction.system.model.user.Bidder;
import com.auction.system.model.user.Seller;
import com.auction.system.server.database.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuctionManagerExtendedTest {

    private static final String TEST_PREFIX = "TEST-EXT-";

    private AuctionManager manager;
    private Seller seller;
    private Bidder bidder;
    private Item item;
    private String sellerId;
    private String bidderId;
    private String itemId;

    @BeforeEach
    void setUp() {
        Database.getInstance().initializeDatabase();
        cleanUpTestData();

        manager = AuctionManager.getInstance();
        manager.resetForTest();

        String token = shortToken();
        sellerId = TEST_PREFIX + "SELLER-" + token;
        bidderId = TEST_PREFIX + "BIDDER-" + token;
        itemId = TEST_PREFIX + "ITEM-" + token;

        seller = new Seller(sellerId, "Ext Seller", "seller_ext_" + token,
                "seller_ext_" + token + "@exttest.local", "secret");

        bidder = new Bidder(bidderId, "Ext Bidder", "bidder_ext_" + token,
                "bidder_ext_" + token + "@exttest.local", "secret");
        bidder.setBalance(10_000);

        item = new Item(itemId, "Ext Item", "Extended test item", 500.0, 500.0, AuctionStatus.OPEN);

        manager.registerUser(seller);
        manager.registerUser(bidder);
    }

    @AfterEach
    void tearDown() {
        if (manager != null) {
            manager.resetForTest();
        }
        cleanUpTestData();
    }

    // ---- addItem validation ----

    @Test
    void addItemThrowsWhenItemIsNull() {
        assertThrows(IllegalArgumentException.class, () -> manager.addItem(null, seller));
    }

    @Test
    void addItemThrowsWhenSellerIsNull() {
        assertThrows(IllegalArgumentException.class, () -> manager.addItem(item, null));
    }

    // ---- updateItem ----

    @Test
    void updateItemSucceedsForOwnItemWhenNotRunning() {
        manager.addItem(item, seller);

        Item updated = new Item(itemId, "Updated Name", "Updated Desc", 600.0, 600.0, AuctionStatus.OPEN);
        assertDoesNotThrow(() -> manager.updateItem(updated, seller));

        Item stored = manager.findItemById(itemId).orElseThrow();
        assertEquals("Updated Name", stored.getName());
    }

    @Test
    void updateItemThrowsWhenSellerDoesNotOwnItem() {
        manager.addItem(item, seller);

        String otherId = TEST_PREFIX + "SELLER-OTHER-" + shortToken();
        Seller other = new Seller(otherId, "Other", "other_ext_" + shortToken(),
                "other_ext_" + shortToken() + "@exttest.local", "secret");
        manager.registerUser(other);

        Item updated = new Item(itemId, "Name", "Desc", 500.0, 500.0, AuctionStatus.OPEN);
        assertThrows(IllegalArgumentException.class, () -> manager.updateItem(updated, other));
    }

    @Test
    void updateItemThrowsWhenAuctionIsRunning() {
        manager.addItem(item, seller);
        manager.startAuction(itemId, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(10));

        Item updated = new Item(itemId, "Name", "Desc", 500.0, 500.0, AuctionStatus.RUNNING);
        assertThrows(IllegalStateException.class, () -> manager.updateItem(updated, seller));
    }

    // ---- removeItem ----

    @Test
    void removeItemSucceedsForOwnItemWithNoBids() {
        manager.addItem(item, seller);
        assertDoesNotThrow(() -> manager.removeItem(itemId, seller));
        assertTrue(manager.findItemById(itemId).isEmpty());
    }

    @Test
    void removeItemThrowsWhenSellerDoesNotOwnItem() {
        manager.addItem(item, seller);

        String otherId = TEST_PREFIX + "SELLER-OTH2-" + shortToken();
        Seller other = new Seller(otherId, "Other2", "other2_ext_" + shortToken(),
                "other2_ext_" + shortToken() + "@exttest.local", "secret");
        manager.registerUser(other);

        assertThrows(IllegalArgumentException.class, () -> manager.removeItem(itemId, other));
    }

    @Test
    void removeItemThrowsWhenItemHasBids() {
        manager.addItem(item, seller);
        manager.startAuction(itemId, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(10));
        manager.placeBid(itemId, bidder, 600.0);

        assertThrows(IllegalStateException.class, () -> manager.removeItem(itemId, seller));
    }

    // ---- requestAuctionApproval ----

    @Test
    void requestAuctionApprovalSucceedsWithValidTimes() {
        manager.addItem(item, seller);
        LocalDateTime start = LocalDateTime.now().plusMinutes(5);
        LocalDateTime end = start.plusHours(1);

        assertDoesNotThrow(() -> manager.requestAuctionApproval(itemId, seller, start, end));

        Item stored = manager.findItemById(itemId).orElseThrow();
        assertFalse(stored.isAuctionApproved());
        assertEquals(start, stored.getStartTime());
        assertEquals(end, stored.getEndTime());
    }

    @Test
    void requestAuctionApprovalThrowsWhenEndBeforeStart() {
        manager.addItem(item, seller);
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusMinutes(5);

        assertThrows(IllegalArgumentException.class,
                () -> manager.requestAuctionApproval(itemId, seller, start, end));
    }

    @Test
    void requestAuctionApprovalThrowsWhenSellerDoesNotOwnItem() {
        manager.addItem(item, seller);

        String otherId = TEST_PREFIX + "SELLER-OTH3-" + shortToken();
        Seller other = new Seller(otherId, "Other3", "other3_ext_" + shortToken(),
                "other3_ext_" + shortToken() + "@exttest.local", "secret");
        manager.registerUser(other);

        LocalDateTime start = LocalDateTime.now().plusMinutes(5);
        LocalDateTime end = start.plusHours(1);
        assertThrows(IllegalArgumentException.class,
                () -> manager.requestAuctionApproval(itemId, other, start, end));
    }

    @Test
    void requestAuctionApprovalThrowsWhenAuctionAlreadyRunning() {
        manager.addItem(item, seller);
        manager.startAuction(itemId, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(10));

        LocalDateTime start = LocalDateTime.now().plusMinutes(5);
        LocalDateTime end = start.plusHours(1);
        assertThrows(IllegalStateException.class,
                () -> manager.requestAuctionApproval(itemId, seller, start, end));
    }

    @Test
    void requestAuctionApprovalThrowsOnBlankItemId() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.requestAuctionApproval("", seller,
                        LocalDateTime.now(), LocalDateTime.now().plusHours(1)));
    }

    @Test
    void requestAuctionApprovalThrowsOnNullTimes() {
        manager.addItem(item, seller);
        assertThrows(IllegalArgumentException.class,
                () -> manager.requestAuctionApproval(itemId, seller, null, null));
    }

    @Test
    void placeBidThrowsBeforeAuctionStartTime() {
        manager.addItem(item, seller);
        manager.startAuction(itemId, LocalDateTime.now().plusMinutes(5), LocalDateTime.now().plusMinutes(30));

        assertThrows(IllegalStateException.class,
                () -> manager.placeBid(itemId, bidder, 600.0));
    }

    // ---- cancelAuction ----

    @Test
    void cancelAuctionSucceedsWhenRunning() {
        manager.addItem(item, seller);
        manager.startAuction(itemId, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(10));

        assertDoesNotThrow(() -> manager.cancelAuction(itemId));

        Item stored = manager.findItemById(itemId).orElseThrow();
        assertEquals(AuctionStatus.CANCELED, stored.getStatus());
    }

    @Test
    void cancelAuctionThrowsWhenAlreadyFinished() {
        manager.addItem(item, seller);
        manager.startAuction(itemId, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(10));
        manager.placeBid(itemId, bidder, 600.0);
        manager.finishAuction(itemId);

        assertThrows(IllegalStateException.class, () -> manager.cancelAuction(itemId));
    }

    @Test
    void cancelAuctionThrowsOnBlankItemId() {
        assertThrows(IllegalArgumentException.class, () -> manager.cancelAuction(""));
    }

    // ---- markAsPaid ----

    @Test
    void markAsPaidSucceedsAfterAuctionFinished() {
        manager.addItem(item, seller);
        manager.startAuction(itemId, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(10));
        manager.placeBid(itemId, bidder, 600.0);
        manager.finishAuction(itemId);

        assertDoesNotThrow(() -> manager.markAsPaid(itemId));

        Item stored = manager.findItemById(itemId).orElseThrow();
        assertEquals(AuctionStatus.PAID, stored.getStatus());
    }

    @Test
    void markAsPaidThrowsWhenAuctionNotFinished() {
        manager.addItem(item, seller);
        manager.startAuction(itemId, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(10));

        assertThrows(IllegalStateException.class, () -> manager.markAsPaid(itemId));
    }

    @Test
    void markAsPaidThrowsOnBlankItemId() {
        assertThrows(IllegalArgumentException.class, () -> manager.markAsPaid(""));
    }

    // ---- winnerDecline ----

    @Test
    void winnerDeclineSucceedsWhenCalledByWinner() {
        manager.addItem(item, seller);
        manager.startAuction(itemId, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(10));
        manager.placeBid(itemId, bidder, 600.0);
        manager.finishAuction(itemId);

        assertDoesNotThrow(() -> manager.winnerDecline(itemId, bidderId));

        Item stored = manager.findItemById(itemId).orElseThrow();
        assertEquals(AuctionStatus.CANCELED, stored.getStatus());
        assertNull(stored.getHighestBidderId());
    }

    @Test
    void winnerDeclineThrowsWhenAuctionNotFinished() {
        manager.addItem(item, seller);
        manager.startAuction(itemId, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(10));
        manager.placeBid(itemId, bidder, 600.0);

        assertThrows(IllegalStateException.class, () -> manager.winnerDecline(itemId, bidderId));
    }

    @Test
    void winnerDeclineThrowsWhenCalledByNonWinner() {
        manager.addItem(item, seller);
        manager.startAuction(itemId, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(10));
        manager.placeBid(itemId, bidder, 600.0);
        manager.finishAuction(itemId);

        assertThrows(IllegalArgumentException.class, () -> manager.winnerDecline(itemId, "WRONG-BIDDER-ID"));
    }

    @Test
    void winnerDeclineThrowsOnBlankItemId() {
        assertThrows(IllegalArgumentException.class, () -> manager.winnerDecline("", bidderId));
    }

    // ---- startAuction validation ----

    @Test
    void startAuctionThrowsOnBlankItemId() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.startAuction("", LocalDateTime.now(), LocalDateTime.now().plusHours(1)));
    }

    @Test
    void startAuctionThrowsOnNullTimes() {
        manager.addItem(item, seller);
        assertThrows(IllegalArgumentException.class,
                () -> manager.startAuction(itemId, null, null));
    }

    // ---- findActiveAutoBid ----

    @Test
    void findActiveAutoBidReturnsNullForBlankIds() {
        assertNull(manager.findActiveAutoBid("", "bidder"));
        assertNull(manager.findActiveAutoBid("item", ""));
    }

    // ---- getAllItems / getAllAuctions ----

    @Test
    void getAllItemsReturnsAddedItem() {
        manager.addItem(item, seller);
        assertTrue(manager.getAllItems().stream()
                .anyMatch(i -> i.getId().equals(itemId)));
    }

    @Test
    void getAllAuctionsReturnsNonNull() {
        assertNotNull(manager.getAllAuctions());
    }

    private void cleanUpTestData() {
        String[] statements = {
            "DELETE FROM auto_bids WHERE item_id LIKE 'TEST-EXT-%' OR bidder_id LIKE 'TEST-EXT-%'",
            "DELETE FROM bids WHERE item_id LIKE 'TEST-EXT-%' OR bidder_id LIKE 'TEST-EXT-%'",
            "DELETE FROM auctions WHERE item_id LIKE 'TEST-EXT-%'",
            "DELETE FROM items WHERE id LIKE 'TEST-EXT-%' OR seller_id LIKE 'TEST-EXT-%'",
            "DELETE FROM users WHERE id LIKE 'TEST-EXT-%'"
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
            throw new RuntimeException("Cannot clean extended test data", e);
        }
    }

    private String shortToken() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
