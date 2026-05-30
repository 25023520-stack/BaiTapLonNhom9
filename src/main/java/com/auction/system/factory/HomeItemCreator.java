package com.auction.system.factory;

import com.auction.system.model.item.Home;
import com.auction.system.model.item.Item;

public class HomeItemCreator extends ItemCreator {
    @Override
    public Item createItem(
            String id,
            String name,
            String description,
            double startingPrice,
            String sellerId
    ) {
        return new Home(id, name, description, startingPrice, sellerId);
    }
}
