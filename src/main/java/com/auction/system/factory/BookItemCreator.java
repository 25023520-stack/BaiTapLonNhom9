package com.auction.system.factory;

import com.auction.system.model.item.Book;
import com.auction.system.model.item.Item;

public class BookItemCreator extends ItemCreator {
    @Override
    public Item createItem(
            String id,
            String name,
            String description,
            double startingPrice,
            String sellerId
    ) {
        return new Book(id, name, description, startingPrice, sellerId);
    }
}
