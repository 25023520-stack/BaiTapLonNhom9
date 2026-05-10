package com.auction.system.factory;

import com.auction.system.model.item.Art;
import com.auction.system.model.item.Electronics;
import com.auction.system.model.item.Vehicle;
import com.auction.system.model.item.Item;

public class ItemFactory {
    public static Item createItem(
        String category,
        int id,
        String name,
        String description,
        double startingPrice,
        int sellerId
    ) {
        if (category.equalsIgnoreCase("electronics")) {
            return new Electronics(id, name, description, startingPrice, sellerId);
        }

        if (category.equalsIgnoreCase("Art")) {
            return new Art(id, name, description, startingPrice, sellerId);
        }

        if (category.equalsIgnoreCase("Vehicle")) {
            return new Vehicle(id, name, description, startingPrice, sellerId);
        }

        throw new IllegalArgumentException("Loại sản phẩm không hợp lệ");
    }
    
}
