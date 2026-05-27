package com.auction.system.server.controller;

import com.auction.system.common.payload.BidPayload;
import com.auction.system.common.payload.Payload;
import com.auction.system.common.payload.ResponsePayload;
import com.auction.system.factory.ItemFactory;
import com.auction.system.model.auction.AutoBid;
import com.auction.system.model.auction.Bid;
import com.auction.system.model.item.Item;
import com.auction.system.model.user.Bidder;
import com.auction.system.model.user.Seller;
import com.auction.system.model.user.User;
import com.auction.system.server.manager.AuctionManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

public class AuctionController {
    private final AuctionManager auctionManager = AuctionManager.getInstance();
    private static final Path ITEM_UPLOAD_DIR = Path.of("data", "uploads", "items");

    public ResponsePayload listItems(User user) {
        ResponsePayload response = ResponsePayload.ok("Items retrieved");
        response.put("items", withClientData(auctionManager.getAllItems(), user));
        return response;
    }

    public ResponsePayload addItem(Payload payload, User user) {
        if (!(user instanceof Seller seller)) {
            return ResponsePayload.error("Only sellers can add items");
        }
        String id = payload.getString("id");
        String name = payload.getString("name");
        String description = payload.getString("description");
        String category = Item.normalizeCategory(payload.getString("category"));
        Double startPrice = payload.getDouble("startPrice");
        if (id == null || name == null || description == null || startPrice == null) {
            return ResponsePayload.error("id, name, description, startPrice are required");
        }
        try {
            Item item = ItemFactory.createItem(category, id, name, description, startPrice, seller.getId());
            String imagePath = saveItemImage(id, payload.getString("imageFileName"), payload.getString("imageBase64"));
            item.setImagePath(imagePath);
            auctionManager.addItem(item, seller);
            ResponsePayload resp = ResponsePayload.ok("Item added successfully");
            attachImageData(item);
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
        String category = Item.normalizeCategory(payload.getString("category"));
        Double startPrice = payload.getDouble("startPrice");
        if (id == null || name == null || description == null || startPrice == null) {
            return ResponsePayload.error("id, name, description, startPrice are required");
        }
        try {
            Item item = ItemFactory.createItem(category, id, name, description, startPrice, seller.getId());
            String imagePath = saveItemImage(id, payload.getString("imageFileName"), payload.getString("imageBase64"));
            if (imagePath != null) {
                item.setImagePath(imagePath);
            } else {
                auctionManager.findItemById(id).ifPresent(existing -> item.setImagePath(existing.getImagePath()));
            }
            auctionManager.updateItem(item, seller);
            Item updatedItem = auctionManager.findItemById(id).orElse(item);
            attachImageData(updatedItem);
            ResponsePayload resp = ResponsePayload.ok("Item updated successfully");
            resp.put("item", updatedItem);
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

    public ResponsePayload startAuction(Payload payload, User user) {
        if (!(user instanceof Seller seller)) {
            return ResponsePayload.error("Only sellers can start auctions");
        }

        String itemId = payload.getString("id");
        String startTimeText = payload.getString("startTime");
        String endTimeText = payload.getString("endTime");
        if (itemId == null || startTimeText == null || endTimeText == null) {
            return ResponsePayload.error("id, startTime, endTime are required");
        }

        try {
            Item item = auctionManager.findItemById(itemId)
                    .orElseThrow(() -> new IllegalArgumentException("Item does not exist"));
            if (!seller.getId().equals(item.getSellerId())) {
                return ResponsePayload.error("Seller can only start their own item auction");
            }

            auctionManager.requestAuctionApproval(
                    itemId,
                    seller,
                    LocalDateTime.parse(startTimeText),
                    LocalDateTime.parse(endTimeText)
            );
            Item updatedItem = auctionManager.findItemById(itemId).orElse(item);
            attachImageData(updatedItem);

            ResponsePayload response = ResponsePayload.ok("Auction approval requested successfully");
            response.put("item", updatedItem);
            return response;
        } catch (RuntimeException exception) {
            return ResponsePayload.error(exception.getMessage());
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
        boolean approved = auctionManager.getAuthManager()
                .findById(user.getId())
                .map(User::isApproved)
                .orElse(user.isApproved());
        // Ghi chu: tra kem so du moi nhat de man Seller khong phai goi them request rieng.
        double sellerBalance = auctionManager.getAuthManager()
                .findById(user.getId())
                .map(User::getBalance)
                .orElse(user.getBalance());
        ResponsePayload resp = ResponsePayload.ok("Items retrieved");
        resp.put("items", withImageData(items));
        resp.put("approved", approved);
        resp.put("sellerBalance", sellerBalance);
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
            Bidder latestBidder = auctionManager.getAuthManager()
                    .findById(bidder.getId())
                    .filter(Bidder.class::isInstance)
                    .map(Bidder.class::cast)
                    .orElse(bidder);
            if (latestBidder.getBalance() < amount) {
                return ResponsePayload.error("Bidder balance is not enough for this bid");
            }

            // Server doc lai so du moi nhat tu database de bidder khong can dang xuat sau khi admin duyet nap tien.
            Bid bid = auctionManager.placeBid(itemId, latestBidder, amount);
            ResponsePayload response = ResponsePayload.ok("Bid accepted");
            response.put("bid", bid);
            response.put("itemId", itemId);
            response.put("amount", bid.getAmount());
            response.put("balance", latestBidder.getBalance());
            auctionManager.findItemById(itemId).ifPresent(item -> {
                attachClientData(item, authenticatedUser);
                response.put("item", item);
            });
            return response;
        } catch (RuntimeException exception) {
            return ResponsePayload.error(exception.getMessage());
        }
    }

    public ResponsePayload setAutoBid(Payload payload, User authenticatedUser) {
        if (!(authenticatedUser instanceof Bidder bidder)) {
            return ResponsePayload.error("Please login with a bidder account before enabling auto-bid");
        }

        String itemId = payload.getString("itemId");
        Double maxBid = payload.getDouble("maxBid");
        Double incrementAmount = payload.getDouble("incrementAmount");

        if (itemId == null || itemId.isBlank()) {
            return ResponsePayload.error("Item id is required");
        }

        if (maxBid == null) {
            return ResponsePayload.error("Max bid is required");
        }

        if (incrementAmount == null) {
            return ResponsePayload.error("Increment amount is required");
        }

        try {
            Item item = auctionManager.setAutoBid(itemId, bidder, maxBid, incrementAmount);
            attachClientData(item, authenticatedUser);

            ResponsePayload response = ResponsePayload.ok("Auto-bid enabled successfully");
            response.put("item", item);
            return response;
        } catch (RuntimeException exception) {
            return ResponsePayload.error(exception.getMessage());
        }
    }

    public ResponsePayload markAsPaid(Payload payload, User authenticatedUser) {
        if (!(authenticatedUser instanceof Bidder bidder)) {
            return ResponsePayload.error("Only bidders can pay for won items");
        }
        String itemId = payload.getString("itemId");
        if (itemId == null || itemId.isBlank()) {
            return ResponsePayload.error("Item id is required");
        }
        try {
            Item item = auctionManager.findItemById(itemId)
                    .orElseThrow(() -> new IllegalArgumentException("Item does not exist"));
            if (!bidder.getId().equals(item.getHighestBidderId())) {
                return ResponsePayload.error("Only the auction winner can pay");
            }
            auctionManager.markAsPaid(itemId);
            Item updatedItem = auctionManager.findItemById(itemId).orElse(item);
            attachImageData(updatedItem);
            ResponsePayload resp = ResponsePayload.ok("Payment confirmed successfully");
            resp.put("item", updatedItem);
            return resp;
        } catch (RuntimeException e) {
            return ResponsePayload.error(e.getMessage());
        }
    }

    public ResponsePayload declineWin(Payload payload, User authenticatedUser) {
        if (!(authenticatedUser instanceof Bidder bidder)) {
            return ResponsePayload.error("Only bidders can decline a win");
        }
        String itemId = payload.getString("itemId");
        if (itemId == null || itemId.isBlank()) {
            return ResponsePayload.error("Item id is required");
        }
        try {
            auctionManager.winnerDecline(itemId, bidder.getId());
            Item updatedItem = auctionManager.findItemById(itemId).orElse(null);
            if (updatedItem != null) attachImageData(updatedItem);
            ResponsePayload resp = ResponsePayload.ok("Win declined. The auction has been canceled.");
            if (updatedItem != null) resp.put("item", updatedItem);
            return resp;
        } catch (RuntimeException e) {
            return ResponsePayload.error(e.getMessage());
        }
    }

    public ResponsePayload cancelAutoBid(Payload payload, User authenticatedUser) {
        if (!(authenticatedUser instanceof Bidder bidder)) {
            return ResponsePayload.error("Please login with a bidder account before canceling auto-bid");
        }

        String itemId = payload.getString("itemId");

        if (itemId == null || itemId.isBlank()) {
            return ResponsePayload.error("Item id is required");
        }

        try {
            Item item = auctionManager.cancelAutoBid(itemId, bidder);
            attachClientData(item, authenticatedUser);

            ResponsePayload response = ResponsePayload.ok("Auto-bid canceled successfully");
            response.put("item", item);
            return response;
        } catch (RuntimeException exception) {
            return ResponsePayload.error(exception.getMessage());
        }
    }

    private List<Item> withImageData(List<Item> items) {
        items.forEach(this::attachImageData);
        return items;
    }

    private List<Item> withClientData(List<Item> items, User user) {
        items.forEach(item -> attachClientData(item, user));
        return items;
    }

    private void attachClientData(Item item, User user) {
        attachImageData(item);
        attachAutoBidData(item, user);
    }

    private void attachAutoBidData(Item item, User user) {
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

    private void attachImageData(Item item) {
        if (item == null || item.getImagePath() == null || item.getImagePath().isBlank()) {
            return;
        }

        try {
            Path imagePath = Path.of(item.getImagePath());
            if (Files.exists(imagePath)) {
                item.setImageBase64(Base64.getEncoder().encodeToString(Files.readAllBytes(imagePath)));
            }
        } catch (IOException ignored) {
            item.setImageBase64(null);
        }
    }

    private String saveItemImage(String itemId, String imageFileName, String imageBase64) throws IOException {
        if (imageBase64 == null || imageBase64.isBlank()) {
            return null;
        }

        Files.createDirectories(ITEM_UPLOAD_DIR);
        String extension = getSafeExtension(imageFileName);
        Path target = ITEM_UPLOAD_DIR.resolve(itemId + extension);
        Files.write(target, Base64.getDecoder().decode(imageBase64));
        return target.toString();
    }

    private String getSafeExtension(String fileName) {
        if (fileName == null) {
            return ".jpg";
        }

        String normalized = fileName.toLowerCase();
        if (normalized.endsWith(".png")) {
            return ".png";
        }
        if (normalized.endsWith(".jpeg")) {
            return ".jpeg";
        }
        return ".jpg";
    }
}
