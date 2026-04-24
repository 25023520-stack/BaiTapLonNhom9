package com.auction.system.common.payload;

public class BidPayload extends Payload {
    public BidPayload() {
        super(PayloadType.BID);
    }

    public BidPayload(int itemId, double amount) {
        this();
        put("itemId", itemId);
        put("amount", amount);
    }

    public int getItemId() {
        Integer itemId = getInt("itemId");
        return itemId != null ? itemId : 0;
    }

    public double getAmount() {
        Double amount = getDouble("amount");
        return amount != null ? amount : 0;
    }
}
