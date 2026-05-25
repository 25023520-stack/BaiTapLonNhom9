package com.auction.system.factory;

import com.auction.system.model.item.Art;
import com.auction.system.model.item.Book;
import com.auction.system.model.item.Collectible;
import com.auction.system.model.item.Electronics;
import com.auction.system.model.item.Fashion;
import com.auction.system.model.item.Home;
import com.auction.system.model.item.Vehicle;
import com.auction.system.model.item.Item;

public class ItemFactory {
    public static Item createItem(
        String category,
        String id,
        String name,
        String description,
        double startingPrice,
        String sellerId
    ) {
        String normalizedCategory = Item.normalizeCategory(category);
        return switch (normalizedCategory) {
            case Item.CATEGORY_ELECTRONICS -> new Electronics(id, name, description, startingPrice, sellerId);
            case Item.CATEGORY_ART -> new Art(id, name, description, startingPrice, sellerId);
            case Item.CATEGORY_VEHICLE -> new Vehicle(id, name, description, startingPrice, sellerId);
            case Item.CATEGORY_FASHION -> new Fashion(id, name, description, startingPrice, sellerId);
            case Item.CATEGORY_BOOK -> new Book(id, name, description, startingPrice, sellerId);
            case Item.CATEGORY_HOME -> new Home(id, name, description, startingPrice, sellerId);
            case Item.CATEGORY_COLLECTIBLE -> new Collectible(id, name, description, startingPrice, sellerId);
            default -> new Item(id, name, description, startingPrice, sellerId, Item.DEFAULT_CATEGORY);
        };
    }
    
}
