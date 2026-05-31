package com.auction.system.model;

import com.auction.system.factory.ItemFactory;
import com.auction.system.model.auction.Auction;
import com.auction.system.model.auction.AuctionStatus;
import com.auction.system.model.auction.AutoBid;
import com.auction.system.model.auction.Bid;
import com.auction.system.model.item.Art;
import com.auction.system.model.item.Book;
import com.auction.system.model.item.Collectible;
import com.auction.system.model.item.Electronics;
import com.auction.system.model.item.Fashion;
import com.auction.system.model.item.Home;
import com.auction.system.model.item.Item;
import com.auction.system.model.item.Vehicle;
import com.auction.system.model.user.Admin;
import com.auction.system.model.user.Bidder;
import com.auction.system.model.user.Seller;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ModelTest {

    // ---- Auction ----

    @Test
    void auctionConstructorThrowsOnNullItem() {
        assertThrows(IllegalArgumentException.class, () -> new Auction("AUC-1", null));
    }

    @Test
    void auctionGetIdAndGetItemReturnConstructorValues() {
        Item item = new Item("I-1", "Name", "Desc", 100, 100, AuctionStatus.OPEN);
        Auction auction = new Auction("AUC-1", item);
        assertEquals("AUC-1", auction.getId());
        assertSame(item, auction.getItem());
    }

    @Test
    void auctionFinishAuctionCanBeCalledWithoutError() {
        Item item = new Item("I-2", "Name", "Desc", 100, 100, AuctionStatus.OPEN);
        Auction auction = new Auction("AUC-2", item);
        assertDoesNotThrow(auction::finishAuction);
    }

    @Test
    void auctionSetStatusCanBeCalledWithoutError() {
        Item item = new Item("I-3", "Name", "Desc", 100, 100, AuctionStatus.OPEN);
        Auction auction = new Auction("AUC-3", item);
        assertDoesNotThrow(() -> auction.setStatus(AuctionStatus.RUNNING));
    }

    // ---- Bid ----

    @Test
    void bidConstructorWithTimestampSetsAllFields() {
        Bidder bidder = new Bidder("B-1", "Full Name", "uname", "e@e.com", "pass");
        LocalDateTime now = LocalDateTime.now();
        Bid bid = new Bid("BID-1", bidder, 500.0, now);

        assertEquals("BID-1", bid.getBidId());
        assertEquals("B-1", bid.getBidderId());
        assertSame(bidder, bid.getBidder());
        assertEquals(500.0, bid.getAmount(), 0.001);
        assertEquals(now, bid.getTimestamp());
    }

    @Test
    void bidConstructorWithoutTimestampSetsNowAutomatically() {
        Bidder bidder = new Bidder("B-2", "Full", "uname2", "e2@e.com", "pass");
        Bid bid = new Bid("BID-2", bidder, 300.0);
        assertNotNull(bid.getTimestamp());
    }

    @Test
    void bidTypeIsManualForRegularBidId() {
        Bidder bidder = new Bidder("B-3", "Full", "uname3", "e3@e.com", "pass");
        Bid bid = new Bid("BID-MANUAL", bidder, 100.0);
        assertEquals("MANUAL", bid.getBidType());
    }

    @Test
    void bidTypeIsAutoForAutoBidTransactionPrefix() {
        Bidder bidder = new Bidder("B-4", "Full", "uname4", "e4@e.com", "pass");
        Bid bid = new Bid("AUTO-BID-TRANSACTION-abc123", bidder, 200.0, LocalDateTime.now());
        assertEquals("AUTO", bid.getBidType());
    }

    @Test
    void bidGetBidderIdReturnsNullWhenBidderIsNull() {
        Bid bid = new Bid("BID-NULL", null, 100.0, LocalDateTime.now());
        assertNull(bid.getBidderId());
    }

    @Test
    void bidToStringContainsBidId() {
        Bidder bidder = new Bidder("B-5", "Alice", "alice", "a@e.com", "pass");
        Bid bid = new Bid("BID-STR", bidder, 150.0, LocalDateTime.now());
        assertTrue(bid.toString().contains("BID-STR"));
    }

    // ---- AutoBid ----

    @Test
    void autoBidConstructorSetsAllFieldsCorrectly() {
        AutoBid ab = new AutoBid("AB-1", "ITEM-1", "BID-1", "username", 1000.0, 50.0);
        assertEquals("AB-1", ab.getId());
        assertEquals("ITEM-1", ab.getItemId());
        assertEquals("BID-1", ab.getBidderId());
        assertEquals("username", ab.getBidderUsername());
        assertEquals(1000.0, ab.getMaxBid(), 0.001);
        assertEquals(50.0, ab.getIncrementAmount(), 0.001);
        assertTrue(ab.isActive());
        assertNotNull(ab.getCreatedAt());
    }

    @Test
    void autoBidDefaultConstructorAndSettersWork() {
        AutoBid ab = new AutoBid();
        LocalDateTime time = LocalDateTime.now();
        ab.setId("ID");
        ab.setItemId("ITEM");
        ab.setBidderId("BIDDER");
        ab.setBidderUsername("user");
        ab.setMaxBid(500.0);
        ab.setIncrementAmount(25.0);
        ab.setActive(false);
        ab.setCreatedAt(time);

        assertEquals("ID", ab.getId());
        assertEquals("ITEM", ab.getItemId());
        assertEquals("BIDDER", ab.getBidderId());
        assertEquals("user", ab.getBidderUsername());
        assertEquals(500.0, ab.getMaxBid(), 0.001);
        assertEquals(25.0, ab.getIncrementAmount(), 0.001);
        assertFalse(ab.isActive());
        assertEquals(time, ab.getCreatedAt());
    }

    // ---- User / Bidder / Seller / Admin ----

    @Test
    void bidderRoleIsBidder() {
        assertEquals("BIDDER", new Bidder("ID", "Full", "user", "e@e.com", "pass").getRole());
    }

    @Test
    void sellerRoleIsSeller() {
        assertEquals("SELLER", new Seller("ID", "Full", "user", "e@e.com", "pass").getRole());
    }

    @Test
    void adminRoleIsAdmin() {
        assertEquals("ADMIN", new Admin("ID", "Full", "user", "e@e.com", "pass").getRole());
    }

    @Test
    void userCheckPasswordReturnsTrueForCorrectPassword() {
        Bidder bidder = new Bidder("ID", "Full", "user", "e@e.com", "secret");
        assertTrue(bidder.checkPassword("secret"));
    }

    @Test
    void userCheckPasswordReturnsFalseForWrongAndNullPassword() {
        Bidder bidder = new Bidder("ID", "Full", "user", "e@e.com", "secret");
        assertFalse(bidder.checkPassword("wrong"));
        assertFalse(bidder.checkPassword(null));
    }

    @Test
    void userSettersAndGettersWork() {
        Bidder bidder = new Bidder("ID", "Full", "user", "e@e.com", "pass");
        bidder.setBalance(500.0);
        bidder.setApproved(false);
        bidder.setEmail("new@e.com");
        bidder.setFullName("New Full");
        bidder.setUserName("newuser");
        bidder.setPassWord("newpass");
        bidder.setId("NEW-ID");

        assertEquals(500.0, bidder.getBalance(), 0.001);
        assertFalse(bidder.isApproved());
        assertEquals("new@e.com", bidder.getEmail());
        assertEquals("New Full", bidder.getFullName());
        assertEquals("newuser", bidder.getUserName());
        assertEquals("NEW-ID", bidder.getId());
    }

    @Test
    void userDefaultApprovedIsTrue() {
        assertTrue(new Bidder("ID", "Full", "user", "e@e.com", "pass").isApproved());
        assertTrue(new Admin("ID", "Full", "user", "e@e.com", "pass").isApproved());
    }

    // ---- Seller specific methods ----

    @Test
    void sellerAddItemForSaleThrowsOnNullItem() {
        Seller seller = new Seller("S-1", "Seller", "seller1", "s@e.com", "pass");
        assertThrows(IllegalArgumentException.class, () -> seller.addItemForSale(null));
    }

    @Test
    void sellerAddItemForSaleThrowsWhenItemBelongsToAnotherSeller() {
        Seller seller = new Seller("S-2", "Seller", "seller2", "s2@e.com", "pass");
        Item item = new Item("I-10", "Name", "Desc", 100, "OTHER-SELLER");
        assertThrows(IllegalArgumentException.class, () -> seller.addItemForSale(item));
    }

    @Test
    void sellerAddItemForSaleSucceedsWhenItemBelongsToSeller() {
        Seller seller = new Seller("S-3", "Seller", "seller3", "s3@e.com", "pass");
        Item item = new Item("I-11", "Name", "Desc", 100, "S-3");
        seller.addItemForSale(item);
        assertEquals(1, seller.getItemsForSale().size());
        assertEquals(1, seller.getItemsForSale().size());
        assertEquals(1, seller.getItems().size());
    }

    @Test
    void sellerRemoveItemForSaleThrowsOnNullItem() {
        Seller seller = new Seller("S-4", "Seller", "seller4", "s4@e.com", "pass");
        assertThrows(IllegalArgumentException.class, () -> seller.removeItemForSale(null));
    }

    @Test
    void sellerRemoveItemForSaleThrowsWhenItemBelongsToAnotherSeller() {
        Seller seller = new Seller("S-5", "Seller", "seller5", "s5@e.com", "pass");
        Item item = new Item("I-12", "Name", "Desc", 100, "OTHER");
        assertThrows(IllegalArgumentException.class, () -> seller.removeItemForSale(item));
    }

    @Test
    void sellerRemoveItemForSaleSucceedsForOwnItem() {
        Seller seller = new Seller("S-6", "Seller", "seller6", "s6@e.com", "pass");
        Item item = new Item("I-13", "Name", "Desc", 100, "S-6");
        seller.addItemForSale(item);
        seller.removeItemForSale(item);
        assertTrue(seller.getItemsForSale().isEmpty());
    }

    @Test
    void sellerUpdateItemReplacesMatchingItem() {
        Seller seller = new Seller("S-7", "Seller", "seller7", "s7@e.com", "pass");
        Item original = new Item("I-14", "Original", "Desc", 100, "S-7");
        seller.addItemForSale(original);

        Item updated = new Item("I-14", "Updated Name", "New Desc", 200, 200, AuctionStatus.OPEN);
        seller.updateItem(updated);

        assertEquals("Updated Name", seller.getItemsForSale().get(0).getName());
    }

    // ---- ItemFactory ----

    @Test
    void itemFactoryCreatesElectronics() {
        Item item = ItemFactory.createItem("electronics", "E-1", "Phone", "Desc", 500.0, "S-1");
        assertInstanceOf(Electronics.class, item);
        assertEquals(Item.CATEGORY_ELECTRONICS, item.getCategory());
    }

    @Test
    void itemFactoryCreatesArt() {
        Item item = ItemFactory.createItem("Art", "A-1", "Painting", "Desc", 300.0, "S-1");
        assertInstanceOf(Art.class, item);
        assertEquals(Item.CATEGORY_ART, item.getCategory());
    }

    @Test
    void itemFactoryCreatesVehicle() {
        Item item = ItemFactory.createItem("Vehicle", "V-1", "Car", "Desc", 10000.0, "S-1");
        assertInstanceOf(Vehicle.class, item);
        assertEquals(Item.CATEGORY_VEHICLE, item.getCategory());
    }

    @Test
    void itemFactoryCreatesFashion() {
        Item item = ItemFactory.createItem("Fashion", "F-1", "Jacket", "Desc", 200.0, "S-1");
        assertInstanceOf(Fashion.class, item);
        assertEquals(Item.CATEGORY_FASHION, item.getCategory());
    }

    @Test
    void itemFactoryCreatesBook() {
        Item item = ItemFactory.createItem("Book", "B-1", "Novel", "Desc", 80.0, "S-1");
        assertInstanceOf(Book.class, item);
        assertEquals(Item.CATEGORY_BOOK, item.getCategory());
    }

    @Test
    void itemFactoryCreatesHome() {
        Item item = ItemFactory.createItem("Home", "H-1", "Chair", "Desc", 120.0, "S-1");
        assertInstanceOf(Home.class, item);
        assertEquals(Item.CATEGORY_HOME, item.getCategory());
    }

    @Test
    void itemFactoryCreatesCollectible() {
        Item item = ItemFactory.createItem("Collectible", "C-1", "Coin", "Desc", 60.0, "S-1");
        assertInstanceOf(Collectible.class, item);
        assertEquals(Item.CATEGORY_COLLECTIBLE, item.getCategory());
    }

    @Test
    void itemFactoryCreatesBaseItemForOther() {
        Item item = ItemFactory.createItem("Other", "O-1", "Misc", "Desc", 100.0, "S-1");
        assertEquals(Item.class, item.getClass());
        assertEquals(Item.DEFAULT_CATEGORY, item.getCategory());
    }

    @Test
    void itemFactoryFallsBackToOtherForInvalidCategory() {
        Item item = ItemFactory.createItem("INVALID", "X-1", "Name", "Desc", 100.0, "S-1");
        assertEquals(Item.class, item.getClass());
        assertEquals(Item.DEFAULT_CATEGORY, item.getCategory());
    }

    // ---- Item ----

    @Test
    void itemConstructorWithStatusSetsFieldsCorrectly() {
        Item item = new Item("ID", "Name", "Desc", 100.0, 120.0, AuctionStatus.RUNNING);
        assertEquals("ID", item.getId());
        assertEquals("Name", item.getName());
        assertEquals("Desc", item.getDescription());
        assertEquals(100.0, item.getStartPrice(), 0.001);
        assertEquals(120.0, item.getCurrentPrice(), 0.001);
        assertEquals(AuctionStatus.RUNNING, item.getStatus());
    }

    @Test
    void itemConstructorDefaultsCurrentPriceToStartPriceWhenZeroOrNegative() {
        Item item = new Item("I-20", "Name", "Desc", 500.0, 0, AuctionStatus.OPEN);
        assertEquals(500.0, item.getCurrentPrice(), 0.001);
    }

    @Test
    void itemConstructorDefaultsStatusToOpenWhenNull() {
        Item item = new Item("I-21", "Name", "Desc", 100.0, 100.0, null);
        assertEquals(AuctionStatus.OPEN, item.getStatus());
    }

    @Test
    void itemAddBidIncreasesBidHistorySize() {
        Item item = new Item("I-22", "Name", "Desc", 100.0, 100.0, AuctionStatus.RUNNING);
        Bidder bidder = new Bidder("B-22", "Full", "u22", "u22@e.com", "pass");
        Bid bid = new Bid("BID-22", bidder, 150.0, LocalDateTime.now());
        item.addBid(bid);
        assertEquals(1, item.getBidHistory().size());
    }

    @Test
    void itemSettersWork() {
        Item item = new Item();
        item.setName("Name");
        item.setDescription("Desc");
        item.setStartPrice(100.0);
        item.setCurrentPrice(150.0);
        item.setStatus(AuctionStatus.FINISHED);
        item.setSellerId("S-1");
        item.setSellerUsername("seller");
        item.setHighestBidderId("B-1");
        item.setHighestBidderUsername("bidder");
        item.setAuctionApproved(true);
        item.setImagePath("/img.png");
        item.setImageBase64("base64data");
        item.setCurrentUserAutoBidActive(true);
        item.setCurrentUserAutoBidMaxBid(200.0);
        item.setCurrentUserAutoBidIncrementAmount(10.0);
        LocalDateTime now = LocalDateTime.now();
        item.setStartTime(now);
        item.setEndTime(now.plusHours(1));

        assertEquals("Name", item.getName());
        assertEquals("S-1", item.getSellerId());
        assertEquals("B-1", item.getHighestBidderId());
        assertTrue(item.isAuctionApproved());
        assertEquals("/img.png", item.getImagePath());
        assertEquals("base64data", item.getImageBase64());
        assertTrue(item.isCurrentUserAutoBidActive());
        assertEquals(200.0, item.getCurrentUserAutoBidMaxBid(), 0.001);
        assertEquals(10.0, item.getCurrentUserAutoBidIncrementAmount(), 0.001);
        assertEquals(now, item.getStartTime());
        assertEquals("seller", item.getSellerUsername());
        assertEquals("bidder", item.getHighestBidderUsername());
    }
}
