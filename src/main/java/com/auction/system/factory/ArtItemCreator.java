package com.auction.system.factory;

import com.auction.system.model.item.Art;
import com.auction.system.model.item.Item;

public class ArtItemCreator extends ItemCreator {
    @Override
    public Item createItem(
            String id,
            String name,
            String description,
            double startingPrice,
            String sellerId
    ) {
        return new Art(id, name, description, startingPrice, sellerId);
    }
}
