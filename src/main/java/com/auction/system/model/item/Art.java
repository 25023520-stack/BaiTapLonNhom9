package com.auction.system.model.item;

public class Art extends Item {
    public Art(int id, String name, String description, double startingPrice, int sellerId) {
        super(id, name, description, startingPrice, sellerId);
    }

    public String getCategory() {
        return "ART";
    }   
}
