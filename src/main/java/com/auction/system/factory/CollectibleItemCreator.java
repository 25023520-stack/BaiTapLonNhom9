package com.auction.system.factory;

import com.auction.system.model.item.Collectible;
import com.auction.system.model.item.Item;

public class CollectibleItemCreator extends ItemCreator {
    @Override
    public Item createItem(
            String id,
            String name,
            String description,
            double startingPrice,
            String sellerId
    ) {
        return new Collectible(id, name, description, startingPrice, sellerId);
    }
}
