package com.auction.system.model.item;

public class Fashion extends Item {
    public Fashion(String id, String name, String description, double startingPrice, String sellerId) {
        super(id, name, description, startingPrice, sellerId, CATEGORY_FASHION);
    }

    @Override
    public String getCategory() {
        return CATEGORY_FASHION;
    }
}
