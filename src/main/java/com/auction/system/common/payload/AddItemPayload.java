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
}