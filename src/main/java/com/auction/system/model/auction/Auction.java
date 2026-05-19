package com.auction.system.model.auction;

import com.auction.system.model.item.Item;

import java.io.Serializable;
import java.util.ArrayList;


public class Auction implements Serializable {
    private String id;
    private Item item;
    private AuctionStatus status;

    public Auction(String id, Item item) {
        if (item == null) {
            throw new IllegalArgumentException("Item không được để trống");
        }
        this.id = id;
        this.item = item;
        this.status = AuctionStatus.OPEN;

    }

    public void finishAuction() {
        status = AuctionStatus.FINISHED;
    }

    public String getId() {
        return id;
    }
    public Item getItem() {
        return item;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

}
