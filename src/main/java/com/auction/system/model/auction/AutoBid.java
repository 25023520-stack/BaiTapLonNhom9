package com.auction.system.model.auction;

import com.auction.system.common.money.Money;

import java.io.Serializable;
import java.time.LocalDateTime;

public class AutoBid implements Serializable {
    private String id;
    private String itemId;
    private String bidderId;
    private String bidderUsername;
    private double incrementAmount;
    private double maxBid;
    private LocalDateTime createdAt;
    private boolean active;

    public AutoBid() {}

    public AutoBid(String id,
                   String itemId,
                   String bidderId,
                   String bidderUsername,
                   double maxBid,
                   double incrementAmount) {
        this.id = id;
        this.itemId = itemId;
        this.bidderId = bidderId;
        this.bidderUsername = bidderUsername;
        setMaxBid(maxBid);
        setIncrementAmount(incrementAmount);
        this.createdAt = LocalDateTime.now();
        this.active = true;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getBidderId() {
        return bidderId;
    }

    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }

    public String getBidderUsername() {
        return bidderUsername;
    }

    public void setBidderUsername(String bidderUsername) {
        this.bidderUsername = bidderUsername;
    }

    public double getMaxBid() {
        return maxBid;
    }

    public void setMaxBid(double maxBid) {
        this.maxBid = Money.normalize(maxBid);
    }

    public double getIncrementAmount() {
        return incrementAmount;
    }

    public void setIncrementAmount(double incrementAmount) {
        this.incrementAmount = Money.normalize(incrementAmount);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

}
