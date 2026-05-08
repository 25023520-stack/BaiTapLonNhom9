package com.auction.system.server.controller;

import com.auction.system.common.payload.BidPayload;
import com.auction.system.common.payload.Payload;
import com.auction.system.common.payload.ResponsePayload;
import com.auction.system.model.auction.Bid;
import com.auction.system.model.user.Bidder;
import com.auction.system.model.user.User;
import com.auction.system.server.manager.AuctionManager;

public class AuctionController {
    private final AuctionManager auctionManager = AuctionManager.getInstance();

    public ResponsePayload listItems() {
        ResponsePayload response = ResponsePayload.ok("Items retrieved");
        response.put("items", auctionManager.getAllItems());
        return response;
    }

    public ResponsePayload placeBid(Payload payload, User authenticatedUser) {
        if (!(authenticatedUser instanceof Bidder bidder)) {
            return ResponsePayload.error("Please login with a bidder account before placing a bid");
        }

        int itemId; // Đã sửa: String -> int
        double amount;
        if (payload instanceof BidPayload bidPayload) {
            itemId = bidPayload.getItemId();
            amount = bidPayload.getAmount();
        } else {
            Integer parsedItemId = payload.getInt("itemId"); // Đã sửa: getString -> getInt
            Double parsedAmount = payload.getDouble("amount");
            if (parsedItemId == null || parsedAmount == null) {
                return ResponsePayload.error("Bid payload must contain itemId and amount");
            }
            itemId = parsedItemId;
            amount = parsedAmount;
        }

        try {
            Bid bid = auctionManager.placeBid(itemId, bidder, amount);
            ResponsePayload response = ResponsePayload.ok("Bid accepted");
            response.put("bid", bid);
            response.put("itemId", itemId);
            response.put("amount", bid.getAmount());
            auctionManager.findItemById(itemId).ifPresent(item -> response.put("item", item));
            return response;
        } catch (RuntimeException exception) {
            return ResponsePayload.error(exception.getMessage());
        }
    }
}