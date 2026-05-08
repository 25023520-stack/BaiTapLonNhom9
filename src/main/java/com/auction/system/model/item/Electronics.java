package com.auction.system.model.item;

public class Electronics extends Item {
    public Electronics(int id, String name, String description, double startingPrice, int sellerId) {
        super(id, name, description, startingPrice, sellerId);
    }

    public String getCategory() {
        return "ELECTRONICS";
    }

    
}
