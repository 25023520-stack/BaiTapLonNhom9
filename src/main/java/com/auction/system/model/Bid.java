package com.auction.system.model;

import java.time.LocalDateTime;
import java.util.Objects;

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

    @Override
    public String toString() {
        return "Bid{" +
                "bidId='" + bidId + '\'' +
                ", bidder=" + bidder +
                ", amount=" + amount +
                ", auction=" + auction +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bid bid = (Bid) o;
        return Objects.equals(bidId, bid.bidId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bidId);
    }
}