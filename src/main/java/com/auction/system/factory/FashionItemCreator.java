package com.auction.system.factory;

import com.auction.system.model.item.Fashion;
import com.auction.system.model.item.Item;

public class FashionItemCreator extends ItemCreator {
    @Override
    public Item createItem(
            String id,
            String name,
            String description,
            double startingPrice,
            String sellerId
    ) {
        return new Fashion(id, name, description, startingPrice, sellerId);
    }
}
