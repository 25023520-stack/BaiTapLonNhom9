package com.auction.system.server.controller;

import com.auction.system.common.payload.Payload;
import com.auction.system.common.payload.ResponsePayload;
import com.auction.system.model.item.Item;
import com.auction.system.model.user.User;
import com.auction.system.server.manager.AdminManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

public class AdminController {
    private final AdminManager adminManager = AdminManager.getInstance();

    public ResponsePayload dashboard(User authenticatedUser) {
        try {
            List<User> pendingSellers = adminManager.getPendingSellers(authenticatedUser);
            List<Item> pendingAuctions = withImageData(adminManager.getPendingAuctionRequests(authenticatedUser));

            ResponsePayload response = ResponsePayload.ok("Admin dashboard loaded");
            response.put("pendingSellers", pendingSellers);
            response.put("pendingAuctions", pendingAuctions);
            return response;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ResponsePayload.error(exception.getMessage());
        }
    }

    public ResponsePayload approveSeller(Payload payload, User authenticatedUser) {
        String sellerId = payload.getString("sellerId");
        if (sellerId == null || sellerId.isBlank()) {
            return ResponsePayload.error("sellerId is required");
        }
        if (Boolean.FALSE.equals(payload.getBoolean("approved"))) {
            return ResponsePayload.error("Seller rejection is not supported in the current workflow");
        }

        try {
            User seller = adminManager.updateSellerApproval(authenticatedUser, sellerId, true);
            ResponsePayload response = ResponsePayload.ok("Seller approved successfully");
            response.put("seller", seller);
            return response;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ResponsePayload.error(exception.getMessage());
        }
    }

    public ResponsePayload approveAuction(Payload payload, User authenticatedUser) {
        String itemId = payload.getString("itemId");
        Boolean requestedApproval = payload.getBoolean("approved");
        boolean approved = requestedApproval == null || requestedApproval;
        if (itemId == null || itemId.isBlank()) {
            return ResponsePayload.error("itemId is required");
        }

        try {
            Item item = adminManager.updateAuctionApproval(authenticatedUser, itemId, approved);
            attachImageData(item);
            ResponsePayload response = ResponsePayload.ok(
                    approved ? "Auction approved successfully" : "Auction request rejected"
            );
            response.put("item", item);
            response.put("approved", approved);
            return response;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ResponsePayload.error(exception.getMessage());
        }
    }

    private List<Item> withImageData(List<Item> items) {
        items.forEach(this::attachImageData);
        return items;
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
}
