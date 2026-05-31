package com.auction.system.factory;

import com.auction.system.model.item.Item;

public abstract class ItemCreator {
    public abstract Item createItem(
            String id,
            String name,
            String description,
            double startingPrice,
            String sellerId
    );
}
