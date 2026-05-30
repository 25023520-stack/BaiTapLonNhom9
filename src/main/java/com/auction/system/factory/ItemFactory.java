package com.auction.system.factory;

import com.auction.system.model.item.Item;

import java.util.Map;

public class ItemFactory {
    private static final ItemCreator DEFAULT_CREATOR = new OtherItemCreator();

    private static final Map<String, ItemCreator> CREATORS = Map.of(
            Item.CATEGORY_ELECTRONICS, new ElectronicsItemCreator(),
            Item.CATEGORY_ART, new ArtItemCreator(),
            Item.CATEGORY_VEHICLE, new VehicleItemCreator(),
            Item.CATEGORY_FASHION, new FashionItemCreator(),
            Item.CATEGORY_BOOK, new BookItemCreator(),
            Item.CATEGORY_HOME, new HomeItemCreator(),
            Item.CATEGORY_COLLECTIBLE, new CollectibleItemCreator()
    );

    public static Item createItem(
        String category,
        String id,
        String name,
        String description,
        double startingPrice,
        String sellerId
    ) {
        String normalizedCategory = Item.normalizeCategory(category);
        ItemCreator creator = CREATORS.getOrDefault(normalizedCategory, DEFAULT_CREATOR);
        return creator.createItem(id, name, description, startingPrice, sellerId);
    }
}
