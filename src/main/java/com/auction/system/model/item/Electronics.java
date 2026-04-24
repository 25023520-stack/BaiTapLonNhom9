package com.auction.system.model.item;

public class Electronics extends Item {
    public Electronics(String id, String name, String description, double startingPrice, String sellerId) {
        super(id, name, description, startingPrice, sellerId);
    }

    public String getCategory() {
        return "ELECTRONICS";
    }

    
}
