package com.auction.system.server.manager;

import com.auction.system.model.item.Item;
import com.auction.system.model.user.Bidder;
import com.auction.system.model.user.Seller;
import com.auction.system.model.auction.AuctionStatus;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionManagerTest {

    @Test
    void itemConstructorKeepsDescriptionAndStatus() {
        Item item = new Item(1, "Laptop", "Gaming laptop", 1000, 1000, AuctionStatus.OPEN);

        assertEquals("Gaming laptop", item.getDescription());
        assertEquals(AuctionStatus.OPEN, item.getStatus());
    }

    @Test
    void loginChecksHashedPasswordInsteadOfReturningRawPassword() {
        AuctionManager manager = new AuctionManager();
        Seller seller = new Seller(1, "Seller One", "seller1", "seller1@example.com", "secret");

        manager.registerUser(seller);

        assertTrue(manager.login("seller1", "secret").isPresent());
        assertFalse(manager.login("seller1", "wrong-password").isPresent());
    }

    @Test
    void placeBidUpdatesCurrentPriceAndHighestBidder() {
        AuctionManager manager = new AuctionManager();
        Seller seller = new Seller(1, "Seller One", "seller1", "seller1@example.com", "secret");
        Bidder bidder = new Bidder(2, "Bidder One", "bidder1", "bidder1@example.com", "secret");
        Item item = new Item(1, "Phone", "Brand new", 500, 0, AuctionStatus.OPEN);

        manager.registerUser(seller);
        manager.registerUser(bidder);
        manager.addItem(item, seller);
        manager.startAuction(1, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(10));

        manager.placeBid(1, bidder, 650);

        Item storedItem = manager.findItemById(1).orElseThrow();
        assertEquals(650, storedItem.getCurrentPrice());
        assertEquals(2, storedItem.getHighestBidderId());
        assertEquals(1, storedItem.getBidHistory().size());
    }

    @Test
    void placeBidRejectsPriceLowerThanCurrentPrice() {
        AuctionManager manager = new AuctionManager();
        Seller seller = new Seller(1, "Seller One", "seller1", "seller1@example.com", "secret");
        Bidder bidder = new Bidder(2, "Bidder One", "bidder1", "bidder1@example.com", "secret");
        Item item = new Item(1, "Phone", "Brand new", 500, 0, AuctionStatus.OPEN);

        manager.registerUser(seller);
        manager.registerUser(bidder);
        manager.addItem(item, seller);
        manager.startAuction(1, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(10));
        manager.placeBid(1, bidder, 650);

        assertThrows(IllegalArgumentException.class, () -> manager.placeBid(1, bidder, 600));
    }
}
