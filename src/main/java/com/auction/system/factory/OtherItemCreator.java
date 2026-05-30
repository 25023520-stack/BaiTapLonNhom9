package com.auction.system.factory;

import com.auction.system.model.item.Item;

public class OtherItemCreator extends ItemCreator {
    @Override
    public Item createItem(
            String id,
            String name,
            String description,
            double startingPrice,
            String sellerId
    ) {
        return new Item(id, name, description, startingPrice, sellerId, Item.DEFAULT_CATEGORY);
    }
}
