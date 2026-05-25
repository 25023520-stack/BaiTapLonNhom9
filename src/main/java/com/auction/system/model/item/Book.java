package com.auction.system.model.item;

public class Book extends Item {
    public Book(String id, String name, String description, double startingPrice, String sellerId) {
        super(id, name, description, startingPrice, sellerId, CATEGORY_BOOK);
    }

    @Override
    public String getCategory() {
        return CATEGORY_BOOK;
    }
}
