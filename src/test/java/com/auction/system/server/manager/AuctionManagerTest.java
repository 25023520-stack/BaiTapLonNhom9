package com.auction.system.server.manager;

import com.auction.system.model.auction.AuctionStatus;
import com.auction.system.model.item.Item;
import com.auction.system.model.user.Bidder;
import com.auction.system.model.user.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.auction.system.server.database.Database;
import org.junit.jupiter.api.AfterEach;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuctionManagerTest {

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

        manager = AuctionManager.getInstance();
        manager.resetForTest();

        sellerId = "TEST-SELLER-" + UUID.randomUUID().toString().substring(0, 8);;
        bidderId = "TEST-BIDDER-" + UUID.randomUUID().toString().substring(0, 8);;
        itemId = "TEST-ITEM-" + UUID.randomUUID().toString().substring(0, 8);;

        seller = new Seller(
                sellerId,
                "Seller One",
                "seller_" + sellerId,
                sellerId + "@example.com",
                "secret"
        );

        bidder = new Bidder(
                bidderId,
                "Bidder One",
                "bidder_" + bidderId,
                bidderId + "@example.com",
                "secret"
        );

        item = new Item(
                itemId,
                "Phone",
                "Brand new phone",
                500,
                500,
                AuctionStatus.OPEN
        );
    }
    @Test
    void itemConstructorKeepsDescriptionAndStatus() {
        Item testItem = new Item(
                "ITEM-TEST",
                "Laptop",
                "Gaming laptop",
                1000,
                1000,
                AuctionStatus.OPEN
        );

        assertEquals("Gaming laptop", testItem.getDescription());
        assertEquals(AuctionStatus.OPEN, testItem.getStatus());
    }

    @Test
    void startAuctionChangesItemStatusToRunning() {
        prepareItemOnly();

        LocalDateTime startTime = LocalDateTime.now().minusMinutes(1);
        LocalDateTime endTime = LocalDateTime.now().plusMinutes(10);

        manager.startAuction(itemId, startTime, endTime);

        Item storedItem = manager.findItemById(itemId).orElseThrow();

        assertEquals(AuctionStatus.RUNNING, storedItem.getStatus());
        assertEquals(startTime, storedItem.getStartTime());
        assertEquals(endTime, storedItem.getEndTime());
    }

    @Test
    void startAuctionRejectsInvalidTimeRange() {
        prepareItemOnly();

        LocalDateTime startTime = LocalDateTime.now().plusMinutes(10);
        LocalDateTime endTime = LocalDateTime.now().minusMinutes(1);

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.startAuction(itemId, startTime, endTime)
        );
    }

    @Test
    void placeBidUpdatesCurrentPriceAndHighestBidder() {
        prepareRunningAuction();

        manager.placeBid(itemId, bidder, 650);

        Item storedItem = manager.findItemById(itemId).orElseThrow();

        assertEquals(650, storedItem.getCurrentPrice());
        assertEquals(bidderId, storedItem.getHighestBidderId());
    }

    @Test
    void placeBidAddsBidToBidHistory() {
        prepareRunningAuction();

        manager.placeBid(itemId, bidder, 650);

        Item storedItem = manager.findItemById(itemId).orElseThrow();

        assertEquals(1, storedItem.getBidHistory().size());
        assertEquals(650, storedItem.getBidHistory().get(0).getAmount());
        assertEquals(bidderId, storedItem.getBidHistory().get(0).getBidderId());
    }

    @Test
    void placeBidRejectsPriceLowerThanCurrentPrice() {
        prepareRunningAuction();

        manager.placeBid(itemId, bidder, 650);

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.placeBid(itemId, bidder, 600)
        );
    }

    @Test
    void placeBidRejectsPriceEqualToCurrentPrice() {
        prepareRunningAuction();

        manager.placeBid(itemId, bidder, 650);

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.placeBid(itemId, bidder, 650)
        );
    }

    @Test
    void placeBidRejectsNullBidder() {
        prepareRunningAuction();

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.placeBid(itemId, null, 650)
        );
    }

    @Test
    void placeBidRejectsFinishedAuction() {
        prepareRunningAuction();

        manager.finishAuction(itemId);

        assertThrows(
                IllegalStateException.class,
                () -> manager.placeBid(itemId, bidder, 650)
        );
    }

    @Test
    void placeBidNearEndExtendsAuctionEndTime() {
        prepareItemOnly();

        LocalDateTime startTime = LocalDateTime.now().minusMinutes(1);
        LocalDateTime endTime = LocalDateTime.now().plusSeconds(30);

        manager.startAuction(itemId, startTime, endTime);
        manager.placeBid(itemId, bidder, 650);

        Item storedItem = manager.findItemById(itemId).orElseThrow();

        assertTrue(storedItem.getEndTime().isAfter(endTime));
        assertTrue(Duration.between(LocalDateTime.now(), storedItem.getEndTime()).getSeconds() >= 45);
    }

    @Test
    void placeBidFarFromEndKeepsAuctionEndTime() {
        prepareItemOnly();

        LocalDateTime startTime = LocalDateTime.now().minusMinutes(1);
        LocalDateTime endTime = LocalDateTime.now().plusMinutes(10);

        manager.startAuction(itemId, startTime, endTime);
        manager.placeBid(itemId, bidder, 650);

        Item storedItem = manager.findItemById(itemId).orElseThrow();

        assertEquals(endTime, storedItem.getEndTime());
    }

    @Test
    void finishAuctionChangesItemStatusToFinished() {
        prepareRunningAuction();

        manager.placeBid(itemId, bidder, 650);
        manager.finishAuction(itemId);

        Item storedItem = manager.findItemById(itemId).orElseThrow();

        assertEquals(AuctionStatus.FINISHED, storedItem.getStatus());
    }

    @Test
    void relistAuctionRestartsCanceledAuctionWithoutAdminApproval() {
        prepareRunningAuction();

        manager.placeBid(itemId, bidder, 650);
        manager.finishAuction(itemId);
        manager.winnerDecline(itemId, bidderId);

        LocalDateTime relistStart = LocalDateTime.now().minusMinutes(1);
        LocalDateTime relistEnd = LocalDateTime.now().plusMinutes(15);
        manager.relistAuction(itemId, seller, relistStart, relistEnd);

        Item storedItem = manager.findItemById(itemId).orElseThrow();

        assertEquals(AuctionStatus.RUNNING, storedItem.getStatus());
        assertTrue(storedItem.isAuctionApproved());
        assertEquals(item.getStartPrice(), storedItem.getCurrentPrice());
        assertNull(storedItem.getHighestBidderId());
        assertEquals(relistStart, storedItem.getStartTime());
        assertEquals(relistEnd, storedItem.getEndTime());
    }

    private void prepareItemOnly() {
        manager.registerUser(seller);
        manager.registerUser(bidder);
        manager.addItem(item, seller);
    }

    private void prepareRunningAuction() {
        prepareItemOnly();

        manager.startAuction(
                itemId,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusMinutes(10)
        );
    }

    @AfterEach
    void cleanUpTestData() {
        String deleteBids = """
            DELETE FROM bids
            WHERE item_id LIKE 'TEST-ITEM-%'
               OR bidder_id LIKE 'TEST-BIDDER-%'
            """;

        String deleteAuctions = """
            DELETE FROM auctions
            WHERE item_id LIKE 'TEST-ITEM-%'
            """;

        String deleteItems = """
            DELETE FROM items
            WHERE id LIKE 'TEST-ITEM-%'
            """;

        String deleteUsers = """
            DELETE FROM users
            WHERE id LIKE 'TEST-SELLER-%'
               OR id LIKE 'TEST-BIDDER-%'
            """;

        try (Connection conn = Database.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            try (
                    PreparedStatement deleteBidsStmt = conn.prepareStatement(deleteBids);
                    PreparedStatement deleteAuctionsStmt = conn.prepareStatement(deleteAuctions);
                    PreparedStatement deleteItemsStmt = conn.prepareStatement(deleteItems);
                    PreparedStatement deleteUsersStmt = conn.prepareStatement(deleteUsers)
            ) {
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
            throw new RuntimeException("Cannot clean test data", e);
        }
    }
}
