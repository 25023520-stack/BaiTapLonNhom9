package com.auction.system.model.auction;

import java.time.LocalDateTime;

import com.auction.system.model.user.Bidder;

public class Bid {
    private final String bidId;
    private final Bidder bidder;
    private final double amount;
    private final Auction auction;
    private final LocalDateTime timestamp;


    public Bid(String bidId, Bidder bidder, double amount, Auction auction) {
        this.bidId = bidId;
        this.bidder = bidder;
        this.amount = amount;
        this.auction = auction;
        this.timestamp = LocalDateTime.now();
    }
    public String getBidId() {
        return bidId;
    }
    public Bidder getBidder() {
        return bidder;
    }
    public double getAmount() {
        return amount;
    }
    public Auction getAuction() {
        return auction;
    }
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    public String toString() {
        return "Bid" + "BidId: " + bidId + ", Bidder: " + bidder.getFullName() + ", Amount: " + amount + ", Timestamp: " + timestamp;
    }


    

  
}