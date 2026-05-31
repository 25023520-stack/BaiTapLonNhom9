package com.auction.system.factory;

import com.auction.system.model.item.Item;
import com.auction.system.model.item.Vehicle;

public class VehicleItemCreator extends ItemCreator {
    @Override
    public Item createItem(
            String id,
            String name,
            String description,
            double startingPrice,
            String sellerId
    ) {
        return new Vehicle(id, name, description, startingPrice, sellerId);
    }
}
