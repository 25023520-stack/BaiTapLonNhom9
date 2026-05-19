package com.auction.system.common.payload;

public class AddItemPayload extends Payload {
    public AddItemPayload() {
        super(PayloadType.ADD_ITEM);
    }

    public AddItemPayload(String id, String name, String description, double startPrice, String sellerId) {
        this();
        put("id", id);
        put("name", name);
        put("description", description);
        put("startPrice", startPrice);
        put("sellerId", sellerId);
    }

    public AddItemPayload(String id, String name, String description, double startPrice, String sellerId,
                          String imageFileName, String imageBase64) {
        this(id, name, description, startPrice, sellerId);
        put("imageFileName", imageFileName);
        put("imageBase64", imageBase64);
    }
}
