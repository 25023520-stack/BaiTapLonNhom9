package com.auction.system.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Bid implements Serializable {
    private final String bidderId;
    private final double amount;
    private final LocalDateTime createdAt;

    public Bid(String bidderId, double amount, LocalDateTime createdAt) {
        this.bidderId = bidderId;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public String getBidderId() {
        return bidderId;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
