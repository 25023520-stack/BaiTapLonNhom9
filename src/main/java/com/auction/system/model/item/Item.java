package com.auction.system.model.item;

import com.auction.system.model.auction.Bid;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.auction.system.model.auction.AuctionStatus;

public class Item implements Serializable {
    private String id;
    private String name, description;
    private double startPrice, currentPrice;
    private AuctionStatus status;
    private String sellerId;
    private String highestBidderId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private final List<Bid> bidHistory = new ArrayList<>();

    public Item() {}

    public Item(String id, String name, String description, double startingPrice, String sellerId) {
        this.id = id;
        this.name = name; 
        this.description = description;
        this.startPrice = startingPrice;
        this.sellerId = sellerId;
    }

    public Item(String id, String name, String description, double startPrice, double currentPrice, AuctionStatus status)  {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startPrice = startPrice;
        this.currentPrice = currentPrice > 0 ? currentPrice : startPrice;
        this.status = status != null ? status : AuctionStatus.OPEN;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public double getStartPrice() {
        return startPrice;
    }

    public void setStartPrice(double startPrice) {
        this.startPrice = startPrice;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getHighestBidderId() {
        return highestBidderId;
    }

    public void setHighestBidderId(String highestBidderId) {
        this.highestBidderId = highestBidderId;
    }

    public LocalDateTime   getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public List<Bid> getBidHistory() {
        return Collections.unmodifiableList(bidHistory);
    }

    public void addBid(Bid bid) {
        bidHistory.add(bid);
    }
}
