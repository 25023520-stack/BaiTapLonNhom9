package com.auction.system.server.observer;

import com.auction.system.model.item.Item;

public interface AuctionObserver {
    void onAuctionUpdated(Item item, String eventType);
    void onBalanceUpdated(String userId, double balance);
}