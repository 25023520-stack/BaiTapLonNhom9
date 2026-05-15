package com.auction.system.server.database;

import com.auction.system.model.auction.AuctionStatus;
import com.auction.system.model.item.Item;
import com.auction.system.server.dao.ItemDAO;

import java.util.UUID;

public class TestItemDAO {
    public static void main(String[] args) {
        Database.getInstance().initializeDatabase();

        ItemDAO itemDAO = new ItemDAO();

        String sellerId = "SELLER-10"; // sửa nếu seller_id của bạn khác
        String itemId = "ITEM-TEST-" + UUID.randomUUID();

        Item item = new Item(
                itemId,
                "Item DAO Test",
                "Test insert item bang ItemDAO",
                1000000,
                sellerId
        );

        item.setCurrentPrice(item.getStartPrice());
        item.setStatus(AuctionStatus.OPEN);

        System.out.println("=== TEST INSERT ===");
        boolean inserted = itemDAO.insertItem(item);
        System.out.println("Inserted: " + inserted);

        System.out.println("=== TEST FIND BY ID ===");
        Item found = itemDAO.findById(itemId);
        System.out.println("Found: " + (found != null));

        if (found != null) {
            System.out.println("Name: " + found.getName());
            System.out.println("Current price: " + found.getCurrentPrice());
            System.out.println("Status: " + found.getStatus());
        }

        System.out.println("=== TEST UPDATE ===");
        if (found != null) {
            found.setName("Item DAO Test Updated");
            found.setDescription("Da update bang ItemDAO");
            found.setStartPrice(2000000);
            found.setCurrentPrice(2000000);

            boolean updated = itemDAO.updateItem(found);
            System.out.println("Updated: " + updated);
        }

        System.out.println("=== TEST DELETE ===");
        boolean deleted = itemDAO.deleteItem(itemId);
        System.out.println("Deleted: " + deleted);

        System.out.println("=== TEST FIND AFTER DELETE ===");
        Item afterDelete = itemDAO.findById(itemId);
        System.out.println("Found after delete: " + (afterDelete != null));
    }
}