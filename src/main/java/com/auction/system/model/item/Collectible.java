package com.auction.system.model.item;

public class Collectible extends Item {
    public Collectible(String id, String name, String description, double startingPrice, String sellerId) {
        super(id, name, description, startingPrice, sellerId, CATEGORY_COLLECTIBLE);
    }

    @Override
    public String getCategory() {
        return CATEGORY_COLLECTIBLE;
    }
}
