package com.auction.system.server.controller;

import com.auction.system.common.payload.Payload;
import com.auction.system.common.payload.PayloadType;
import com.auction.system.common.payload.ResponsePayload;
import com.auction.system.model.item.Item;
import com.auction.system.model.user.Seller;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AuctionControllerTest {

    @Test
    void addItemRejectsMissingImage() {
        AuctionController controller = new AuctionController();
        Seller seller = new Seller("TEST-CTRL-SELLER", "Seller", "seller_ctrl",
                "seller_ctrl@example.com", "secret");

        Payload payload = new Payload(PayloadType.ADD_ITEM);
        payload.put("id", "TEST-CTRL-ITEM");
        payload.put("name", "Item without image");
        payload.put("description", "Description");
        payload.put("startPrice", 500.0);
        payload.put("sellerId", seller.getId());
        payload.put("category", Item.DEFAULT_CATEGORY);

        ResponsePayload response = controller.addItem(payload, seller);

        assertFalse(response.isSuccess());
        assertEquals("Product image is required", response.getMessage());
    }
}
