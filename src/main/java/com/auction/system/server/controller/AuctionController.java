package com.auction.system.server.controller;

import com.auction.system.common.payload.BidPayload;
import com.auction.system.common.payload.Payload;
import com.auction.system.common.payload.ResponsePayload;
import com.auction.system.model.auction.Bid;
import com.auction.system.model.item.Item;
import com.auction.system.model.user.Bidder;
import com.auction.system.model.user.Seller;
import com.auction.system.model.user.User;
import com.auction.system.server.manager.AuctionManager;

import java.util.List;

public class AuctionController {
    private final AuctionManager auctionManager = AuctionManager.getInstance();

    public ResponsePayload listItems() {
        ResponsePayload response = ResponsePayload.ok("Items retrieved");
        response.put("items", auctionManager.getAllItems());
        return response;
    }

    public ResponsePayload addItem(Payload payload, User user) {
        if (!(user instanceof Seller seller)) {
            return ResponsePayload.error("Only sellers can add items");
        }
        String id = payload.getString("id");
        String name = payload.getString("name");
        String description = payload.getString("description");
        Double startPrice = payload.getDouble("startPrice");
        if (id == null || name == null || description == null || startPrice == null) {
            return ResponsePayload.error("id, name, description, startPrice are required");
        }
        try {
            Item item = new Item(id, name, description, startPrice, seller.getId());
            auctionManager.addItem(item, seller);
            ResponsePayload resp = ResponsePayload.ok("Item added successfully");
            resp.put("item", item);
            return resp;
        } catch (Exception e) {
            return ResponsePayload.error(e.getMessage());
        }
    }

    public ResponsePayload updateItem(Payload payload, User user) {
        if (!(user instanceof Seller seller)) {
            return ResponsePayload.error("Only sellers can update items");
        }
        String id = payload.getString("id");
        String name = payload.getString("name");
        String description = payload.getString("description");
        Double startPrice = payload.getDouble("startPrice");
        if (id == null || name == null || description == null || startPrice == null) {
            return ResponsePayload.error("id, name, description, startPrice are required");
        }
        try {
            Item item = new Item(id, name, description, startPrice, seller.getId());
            auctionManager.updateItem(item, seller);
            ResponsePayload resp = ResponsePayload.ok("Item updated successfully");
            resp.put("item", item);
            return resp;
        } catch (Exception e) {
            return ResponsePayload.error(e.getMessage());
        }
    }

    public ResponsePayload removeItem(Payload payload, User user) {
        if (!(user instanceof Seller seller)) {
            return ResponsePayload.error("Only sellers can remove items");
        }
        String id = payload.getString("id");
        if (id == null) {
            return ResponsePayload.error("Item id is required");
        }
        try {
            auctionManager.removeItem(id, seller);
            return ResponsePayload.ok("Item removed successfully");
        } catch (Exception e) {
            return ResponsePayload.error(e.getMessage());
        }
    }

    public ResponsePayload listItemsBySeller(Payload payload, User user) {
        if (!(user instanceof Seller)) {
            return ResponsePayload.error("Only sellers can list their items");
        }
        String sellerId = payload.getString("sellerId");
        if (sellerId == null) {
            return ResponsePayload.error("sellerId is required");
        }
        List<Item> items = auctionManager.getAllItems().stream()
                .filter(item -> sellerId.equals(item.getSellerId()))
                .toList();
        ResponsePayload resp = ResponsePayload.ok("Items retrieved");
        resp.put("items", items);
        return resp;
    }

    public ResponsePayload placeBid(Payload payload, User authenticatedUser) {
        if (!(authenticatedUser instanceof Bidder bidder)) {
            return ResponsePayload.error("Please login with a bidder account before placing a bid");
        }

        String itemId;
        double amount;
        if (payload instanceof BidPayload bidPayload) {
            itemId = bidPayload.getItemId();
            amount = bidPayload.getAmount();
        } else {
            String parsedItemId = payload.getString("itemId");
            Double parsedAmount = payload.getDouble("amount");
            if (parsedItemId == null || parsedItemId.isBlank() || parsedAmount == null) {
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
