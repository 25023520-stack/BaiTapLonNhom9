package com.auction.system.model.item;

public class Art extends Item {
    public Art(String id, String name, String description, double startingPrice, String sellerId) {
        super(id, name, description, startingPrice, sellerId, CATEGORY_ART);
    }

    @Override
    public String getCategory() {
        return CATEGORY_ART;
    }   
}
