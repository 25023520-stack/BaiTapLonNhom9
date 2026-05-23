package com.auction.system.server.manager;

import com.auction.system.model.auction.AuctionStatus;
import com.auction.system.model.auction.AutoBid;
import com.auction.system.model.item.Item;
import com.auction.system.model.user.Bidder;
import com.auction.system.model.user.Seller;
import com.auction.system.server.dao.AutoBidDAO;
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

class AuctionManagerAutoBidTest {
    private static final String TEST_PREFIX = "TEST-AUTO-";

    private AuctionManager manager;
    private AutoBidDAO autoBidDAO;
    private Seller seller;
    private Item item;
    private String itemId;

    @BeforeEach
    void setUp() {
        Database.getInstance().initializeDatabase();
        cleanUpTestData();

        manager = AuctionManager.getInstance();
        manager.resetForTest();
        autoBidDAO = new AutoBidDAO();

        String token = shortToken();
        itemId = TEST_PREFIX + "ITEM-" + token;
        seller = new Seller(
                TEST_PREFIX + "SELLER-" + token,
                "Auto Seller",
                "seller_auto_" + token,
                "seller_auto_" + token + "@example.com",
                "secret"
        );
        item = new Item(itemId, "Auto Item", "Item for auto-bid tests", 500, 500, AuctionStatus.OPEN);

        manager.registerUser(seller);
        manager.addItem(item, seller);
        manager.startAuction(itemId, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(10));
    }

    @AfterEach
    void tearDown() {
        if (manager != null) {
            manager.resetForTest();
        }
        cleanUpTestData();
    }

    @Test
    void setAutoBidSucceedsWhenAuctionRunning() {
        Bidder bidder = registerBidder("one", 2_000);

        manager.setAutoBid(itemId, bidder, 1_000, 50);

        AutoBid autoBid = autoBidDAO.findActiveByItemAndBidder(itemId, bidder.getId());
        Item storedItem = manager.findItemById(itemId).orElseThrow();
        assertNotNull(autoBid);
        assertEquals(1_000, autoBid.getMaxBid(), 0.001);
        assertEquals(50, autoBid.getIncrementAmount(), 0.001);
        assertEquals(bidder.getId(), storedItem.getHighestBidderId());
        assertEquals(550, storedItem.getCurrentPrice(), 0.001);
    }

    @Test
    void setAutoBidRejectsMaxBidNotAboveCurrentPrice() {
        Bidder bidder = registerBidder("low", 2_000);

        assertThrows(IllegalArgumentException.class, () -> manager.setAutoBid(itemId, bidder, 500, 50));
    }

    @Test
    void setAutoBidRejectsInvalidIncrement() {
        Bidder bidder = registerBidder("increment", 2_000);

        assertThrows(IllegalArgumentException.class, () -> manager.setAutoBid(itemId, bidder, 1_000, 0));
    }

    @Test
    void manualBidTriggersOneAutoBidWithHighestMaxBidWinner() {
        Bidder manual = registerBidder("manual", 2_000);
        Bidder low = registerBidder("lowmax", 2_000);
        Bidder high = registerBidder("highmax", 2_000);

        manager.setAutoBid(itemId, low, 900, 50);
        manager.setAutoBid(itemId, high, 1_000, 50);
        int historySizeBeforeManualBid = manager.findItemById(itemId).orElseThrow().getBidHistory().size();
        manager.placeBid(itemId, manual, 960);

        Item storedItem = manager.findItemById(itemId).orElseThrow();
        assertEquals(high.getId(), storedItem.getHighestBidderId());
        assertEquals(1_000, storedItem.getCurrentPrice(), 0.001);
        assertEquals(historySizeBeforeManualBid + 2, storedItem.getBidHistory().size());
        assertEquals("AUTO", storedItem.getBidHistory().get(storedItem.getBidHistory().size() - 1).getBidType());
    }

    @Test
    void currentLeaderAutoBidRespondsWhenAnotherAutoBidderCompetes() {
        Bidder high = registerBidder("leaderauto", 2_000);
        Bidder low = registerBidder("challengerauto", 2_000);

        manager.setAutoBid(itemId, high, 1_000, 50);
        manager.setAutoBid(itemId, low, 900, 50);

        Item storedItem = manager.findItemById(itemId).orElseThrow();
        assertEquals(high.getId(), storedItem.getHighestBidderId());
        assertEquals(950, storedItem.getCurrentPrice(), 0.001);
        assertEquals("AUTO", storedItem.getBidHistory().get(storedItem.getBidHistory().size() - 1).getBidType());
    }

    @Test
    void equalMaxBidKeepsEarlierAutoBidderAsWinner() {
        Bidder manual = registerBidder("manualtie", 2_000);
        Bidder first = registerBidder("first", 2_000);
        Bidder second = registerBidder("second", 2_000);

        manager.setAutoBid(itemId, first, 1_000, 50);
        manager.setAutoBid(itemId, second, 1_000, 50);

        Item storedItem = manager.findItemById(itemId).orElseThrow();
        assertEquals(first.getId(), storedItem.getHighestBidderId());
        assertEquals(1_000, storedItem.getCurrentPrice(), 0.001);
    }

    @Test
    void updatingAutoBidKeepsCreatedAt() {
        Bidder bidder = registerBidder("update", 2_000);

        manager.setAutoBid(itemId, bidder, 1_000, 50);
        AutoBid first = autoBidDAO.findActiveByItemAndBidder(itemId, bidder.getId());
        manager.setAutoBid(itemId, bidder, 1_200, 75);
        AutoBid updated = autoBidDAO.findActiveByItemAndBidder(itemId, bidder.getId());

        assertNotNull(first);
        assertNotNull(updated);
        assertEquals(first.getCreatedAt(), updated.getCreatedAt());
        assertEquals(1_200, updated.getMaxBid(), 0.001);
        assertEquals(75, updated.getIncrementAmount(), 0.001);
    }

    @Test
    void exhaustedAutoBidBecomesInactive() {
        Bidder manual = registerBidder("manualex", 2_000);
        Bidder auto = registerBidder("autoex", 2_000);

        manager.setAutoBid(itemId, auto, 650, 50);
        manager.placeBid(itemId, manual, 700);

        assertNull(autoBidDAO.findActiveByItemAndBidder(itemId, auto.getId()));
    }

    @Test
    void canceledAutoBidDoesNotRunAfterManualBid() {
        Bidder manual = registerBidder("manualcancel", 2_000);
        Bidder auto = registerBidder("autocancel", 2_000);

        manager.setAutoBid(itemId, auto, 1_000, 50);
        manager.cancelAutoBid(itemId, auto);
        manager.placeBid(itemId, manual, 600);

        Item storedItem = manager.findItemById(itemId).orElseThrow();
        assertEquals(manual.getId(), storedItem.getHighestBidderId());
        assertEquals(600, storedItem.getCurrentPrice(), 0.001);
    }

    @Test
    void sellerCannotBidOrEnableAutoBidOnOwnItem() {
        Bidder sameIdAsSeller = new Bidder(
                seller.getId(),
                "Fake Bidder",
                "fake_" + shortToken(),
                "fake_" + shortToken() + "@example.com",
                "secret"
        );
        sameIdAsSeller.setBalance(2_000);

        assertThrows(IllegalArgumentException.class, () -> manager.placeBid(itemId, sameIdAsSeller, 600));
        assertThrows(IllegalArgumentException.class, () -> manager.setAutoBid(itemId, sameIdAsSeller, 1_000, 50));
    }

    private Bidder registerBidder(String label, double balance) {
        String token = label + "_" + shortToken();
        Bidder bidder = new Bidder(
                TEST_PREFIX + "BIDDER-" + token,
                "Bidder " + label,
                "bidder_auto_" + token,
                "bidder_auto_" + token + "@example.com",
                "secret"
        );
        bidder.setBalance(balance);
        manager.registerUser(bidder);
        return bidder;
    }

    private void cleanUpTestData() {
        String deleteAutoBids = """
                DELETE FROM auto_bids
                WHERE item_id LIKE 'TEST-AUTO-%'
                   OR bidder_id LIKE 'TEST-AUTO-%'
                """;

        String deleteBids = """
                DELETE FROM bids
                WHERE item_id LIKE 'TEST-AUTO-%'
                   OR bidder_id LIKE 'TEST-AUTO-%'
                """;

        String deleteAuctions = """
                DELETE FROM auctions
                WHERE item_id LIKE 'TEST-AUTO-%'
                """;

        String deleteItems = """
                DELETE FROM items
                WHERE id LIKE 'TEST-AUTO-%'
                   OR seller_id LIKE 'TEST-AUTO-%'
                """;

        String deleteUsers = """
                DELETE FROM users
                WHERE id LIKE 'TEST-AUTO-%'
                """;

        try (Connection conn = Database.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            try (
                    PreparedStatement deleteAutoBidsStmt = conn.prepareStatement(deleteAutoBids);
                    PreparedStatement deleteBidsStmt = conn.prepareStatement(deleteBids);
                    PreparedStatement deleteAuctionsStmt = conn.prepareStatement(deleteAuctions);
                    PreparedStatement deleteItemsStmt = conn.prepareStatement(deleteItems);
                    PreparedStatement deleteUsersStmt = conn.prepareStatement(deleteUsers)
            ) {
                deleteAutoBidsStmt.executeUpdate();
                deleteBidsStmt.executeUpdate();
                deleteAuctionsStmt.executeUpdate();
                deleteItemsStmt.executeUpdate();
                deleteUsersStmt.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Cannot clean auto-bid test data", e);
        }
    }

    private String shortToken() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
