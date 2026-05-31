package com.auction.system.server.util;

import com.auction.system.model.auction.AutoBid;
import com.auction.system.model.item.Item;
import com.auction.system.model.user.Bidder;
import com.auction.system.model.user.User;
import com.auction.system.server.manager.AuctionManager;

public class AutoBidEnricher {

    private AutoBidEnricher() {}

    public static void attach(Item item, User user, AuctionManager auctionManager) {
        if (item == null) {
            return;
        }
        item.setCurrentUserAutoBidActive(false);
        item.setCurrentUserAutoBidMaxBid(0);
        item.setCurrentUserAutoBidIncrementAmount(0);

        if (!(user instanceof Bidder bidder)) {
            return;
        }

        AutoBid autoBid = auctionManager.findActiveAutoBid(item.getId(), bidder.getId());
        if (autoBid == null) {
            return;
        }

        item.setCurrentUserAutoBidActive(true);
        item.setCurrentUserAutoBidMaxBid(autoBid.getMaxBid());
        item.setCurrentUserAutoBidIncrementAmount(autoBid.getIncrementAmount());
    }
}
