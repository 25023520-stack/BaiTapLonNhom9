package com.auction.system.factory;

import com.auction.system.model.item.Electronics;
import com.auction.system.model.item.Item;

public class ElectronicsItemCreator extends ItemCreator {
    @Override
    public Item createItem(
            String id,
            String name,
            String description,
            double startingPrice,
            String sellerId
    ) {
        return new Electronics(id, name, description, startingPrice, sellerId);
    }
}
