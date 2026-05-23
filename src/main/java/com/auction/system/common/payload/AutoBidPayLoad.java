package com.auction.system.common.payload;

public class AutoBidPayLoad extends Payload {
    public AutoBidPayLoad() {
        super(PayloadType.AUTO_BID_SET);
    }

    public AutoBidPayLoad(String itemId,
                          double maxBid,
                          double incrementAmount) {
        this();
        setItemId(itemId);
        setMaxBid(maxBid);
        setIncrementAmount(incrementAmount);
    }

    public static AutoBidPayLoad cancel(String itemId) {
        AutoBidPayLoad payload = new AutoBidPayLoad();
        payload.setType(PayloadType.AUTO_BID_CANCEL);
        payload.setItemId(itemId);
        return payload;
    }

    public void setItemId(String itemId) {
        put("itemId", itemId);
    }

    public void setMaxBid(double maxBid) {
        put("maxBid", maxBid);
    }

    public void setIncrementAmount(double incrementAmount) {
        put("incrementAmount", incrementAmount);
    }

    public String getItemId() {
        return getString("itemId");
    }

    public double getMaxBid() {
        Double maxBid = getDouble("maxBid");
        return maxBid != null ? maxBid : 0;
    }

    public double getIncrementAmount() {
        Double incrementAmount = getDouble("incrementAmount");
        return incrementAmount != null ? incrementAmount : 0;
    }
}
