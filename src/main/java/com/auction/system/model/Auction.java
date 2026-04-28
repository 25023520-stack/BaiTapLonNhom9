package com.auction.system.model;

import com.auction.system.model.item.Item;
import com.auction.system.model.user.User;
import com.auction.system.model.auction.AuctionStatus;

public abstract class Auction {
    private final String auctionid;
    private final Item item;
    private final User seller;
    private AuctionStatus status;

    public Auction(String auctionid, Item item, User seller) {
        this.auctionid = auctionid;
        this.item = item;
        this.seller = seller;
        this.status = AuctionStatus.OPEN;
    }

    public String getAuctionId() {
        return auctionid;
    }

    public Item getItem() {
        return item;
    }

    public User getSeller() {
        return seller;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }
}
