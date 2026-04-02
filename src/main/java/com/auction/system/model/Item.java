package com.auction.system.model;

public class Item {
    private int id;
    private String name, description;
    private double startPrice, currentPrice;
    private AuctionStatus status;

    public Item() {}

    public Item(int id, String name, String description, double startPrice, double currentPrice, AuctionStatus status)  {
        this.id = id;
        this.name = name;
        this.startPrice = startPrice;
        this.currentPrice = currentPrice;
        this.status = AuctionStatus.OPEN;
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

    public int getId() {
        return id;
    }
}
