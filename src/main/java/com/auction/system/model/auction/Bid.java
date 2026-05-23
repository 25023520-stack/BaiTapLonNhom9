package com.auction.system.model.auction;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.auction.system.model.user.Bidder;

public class Bid implements Serializable {
    private final String bidId;
    private final Bidder bidder;
    private final double amount;
    private final LocalDateTime timestamp;


    public Bid(String bidId, Bidder bidder, double amount) {
        this.bidId = bidId;
        this.bidder = bidder;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }

    public Bid(String bidId, Bidder bidder, double amount, LocalDateTime timestamp) {
        this.bidId = bidId;
        this.bidder = bidder;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public String getBidId() {
        return bidId;
    }
    public String getBidderId() {
        return bidder != null ? bidder.getId() : null;
    }
    public Bidder getBidder() {
        return bidder;
    }
    public double getAmount() {
        return amount;
    }
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    public String getBidType() {
        return bidId != null && bidId.startsWith("AUTO-BID-TRANSACTION-") ? "AUTO" : "MANUAL";
    }
    public String toString() {
        return "Bid" + "BidId: " + bidId + ", Bidder: " + bidder.getFullName() + ", Amount: " + amount + ", Timestamp: " + timestamp;
    }
}
