package com.auction.system.model.auction;

import com.auction.system.model.item.Item;
import com.auction.system.model.user.Bidder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Auction implements Serializable {
    private int id;
    private Item item;
    private double currentPrice;
    private AuctionStatus status;
    private List<Bid> bids;

    public Auction(int id, Item item) {
        if (item == null) {
            throw new IllegalArgumentException("Item không được để trống");
        }
        this.id = id;
        this.item = item;
        this.currentPrice = item.getCurrentPrice();
        this.status = AuctionStatus.OPEN;
        this.bids = new ArrayList<>();
    }

    public void addBid(Bid bid) {
        if (bid == null) {
            throw new IllegalArgumentException("bid không được để trống");
        
        }
        if (status != AuctionStatus.OPEN) {
            throw new IllegalArgumentException("Phiên đấu giá chưa mở hoặc đã kết thúc");
        }
        if (bid.getAmount() <= currentPrice) {
            throw new IllegalArgumentException("Giá đạt không được thấp hơn giá hiện tại");
        }

        bids.add(bid); 
        currentPrice = bid.getAmount();
    }
    public void finishAuction() {
        status = AuctionStatus.FINISHED;
    }

    public int getId() {
        return id;
    }
    public Item getItem() {
        return item;
    }
    public double getCurrentPrice() {
        return currentPrice;
    }
    public List<Bid> getBids() {
        return bids;
    }
    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

     public void placeBid(Bidder bidder, double amount) {
        if (bidder == null) {
            throw new IllegalArgumentException("Người đặt giá không được để trống");
        }
         if (status != AuctionStatus.OPEN) {
            throw new IllegalArgumentException("Phiên đấu giá chưa bắt đầu hoặc đã kết thúc");
         }
         if (amount <= currentPrice) {
            throw new IllegalArgumentException("Giá đặt phải cao hơn giá hiện tại");
         }
         int bidId = bids.size() + 1;
         Bid bid = new Bid(bidId, bidder, amount) ;

         bids.add(bid);
         currentPrice = amount;
         status = AuctionStatus.RUNNING;
    }
    




    
}
