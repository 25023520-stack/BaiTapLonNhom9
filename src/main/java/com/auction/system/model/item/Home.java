package com.auction.system.model.item;

public class Home extends Item {
    public Home(String id, String name, String description, double startingPrice, String sellerId) {
        super(id, name, description, startingPrice, sellerId, CATEGORY_HOME);
    }

    @Override
    public String getCategory() {
        return CATEGORY_HOME;
    }
}
