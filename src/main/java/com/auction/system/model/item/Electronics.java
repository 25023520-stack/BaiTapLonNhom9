package com.auction.system.model.item;

public class Electronics extends Item {
    public Electronics(String id, String name, String description, double startingPrice, String sellerId) {
        super(id, name, description, startingPrice, sellerId, CATEGORY_ELECTRONICS);
    }

    @Override
    public String getCategory() {
        return CATEGORY_ELECTRONICS;
    }

    
}
