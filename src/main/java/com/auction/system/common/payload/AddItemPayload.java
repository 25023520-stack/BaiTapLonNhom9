package com.auction.system.common.payload;

import com.auction.system.model.item.Item;

public class AddItemPayload extends Payload {
    public AddItemPayload() {
        super(PayloadType.ADD_ITEM);
    }

    public AddItemPayload(String id, String name, String description, double startPrice, String sellerId) {
        this(id, name, description, startPrice, sellerId, Item.DEFAULT_CATEGORY);
    }

    public AddItemPayload(String id, String name, String description, double startPrice, String sellerId,
                          String category) {
        this();
        put("id", id);
        put("name", name);
        put("description", description);
        put("startPrice", startPrice);
        put("sellerId", sellerId);
        put("category", Item.normalizeCategory(category));
    }

    public AddItemPayload(String id, String name, String description, double startPrice, String sellerId,
                          String imageFileName, String imageBase64) {
        this(id, name, description, startPrice, sellerId, Item.DEFAULT_CATEGORY, imageFileName, imageBase64);
    }

    public AddItemPayload(String id, String name, String description, double startPrice, String sellerId,
                          String category, String imageFileName, String imageBase64) {
        this(id, name, description, startPrice, sellerId, category);
        put("imageFileName", imageFileName);
        put("imageBase64", imageBase64);
    }

    public String getCategory() {
        return Item.normalizeCategory(getString("category"));
    }
}
