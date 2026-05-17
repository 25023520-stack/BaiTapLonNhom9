package com.auction.system.server.observer;

public interface AuctionSubject {
    void addObserver(AuctionObserver observer);
    void removeObserver(AuctionObserver observer);
    void notifyObservers(com.auction.system.model.item.Item item, String eventType);
}