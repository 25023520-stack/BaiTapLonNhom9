package com.auction.system.server.manager;

import com.auction.system.exception.InvalidDataException;
import com.auction.system.exception.ItemNotFoundException;
import com.auction.system.factory.ItemFactory;
import com.auction.system.model.auction.AuctionStatus;
import com.auction.system.model.item.Item;
import com.auction.system.server.dao.ItemDAO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemManager {
    private static final ItemManager INSTANCE = new ItemManager(false);

    private final Map<String, Item> itemsById = new HashMap<>();

    private final ItemDAO itemDAO = new ItemDAO();

    private ItemManager() {
        this(false);
    }

    private ItemManager(boolean seedSampleItems) {
        if (seedSampleItems) {
            seedSampleItems();
        }
    }

    public static ItemManager getInstance() {
        return INSTANCE;
    }

    public synchronized void addItem(Item item) throws InvalidDataException {
        validateItemForCreate(item);

        if(item.getCurrentPrice() <= 0) {
            item.setCurrentPrice(item.getStartPrice());
        }

        if (item.getStatus() == null) {
            item.setStatus(AuctionStatus.OPEN);
        }
        item.setCategory(item.getCategory());
        boolean inserted = itemDAO.insertItem(item);

        if(!inserted) {
            throw new InvalidDataException("Cannot save item to database");
        }

        itemsById.put(item.getId(), item);
    }

    public synchronized void loadItemsFromDatabase() {
        itemsById.clear();

        for (Item item : itemDAO.findAll()) {
            itemsById.put(item.getId(), item);
        }
    }

    public synchronized void updateItem(String itemId, String name, String description, double startPrice)
            throws ItemNotFoundException, InvalidDataException {
        updateItem(itemId, name, description, startPrice, null);
    }

    public synchronized void updateItem(String itemId, String name, String description, double startPrice, String imagePath)
            throws ItemNotFoundException, InvalidDataException {
        updateItem(itemId, name, description, startPrice, imagePath, null);
    }

    public synchronized void updateItem(String itemId, String name, String description, double startPrice,
                                        String imagePath, String category)
            throws ItemNotFoundException, InvalidDataException {
        Item existingItem = findItemById(itemId);

        if (isBlank(name)) {
            throw new InvalidDataException("Item name must not be blank");
        }
        if (isBlank(description)) {
            throw new InvalidDataException("Item description must not be blank");
        }
        if (startPrice < 0) {
            throw new InvalidDataException("Start price must be greater than or equal to 0");
        }

        Item updatedItem = ItemFactory.createItem(
                category != null ? category : existingItem.getCategory(),
                existingItem.getId(),
                name.trim(),
                description.trim(),
                startPrice,
                existingItem.getSellerId()
        );
        updatedItem.setImagePath(imagePath != null ? imagePath : existingItem.getImagePath());
        updatedItem.setCurrentPrice(startPrice);
        updatedItem.setStatus(existingItem.getStatus());
        updatedItem.setSellerUsername(existingItem.getSellerUsername());
        updatedItem.setHighestBidderId(existingItem.getHighestBidderId());
        updatedItem.setHighestBidderUsername(existingItem.getHighestBidderUsername());
        updatedItem.setAuctionApproved(existingItem.isAuctionApproved());
        updatedItem.setStartTime(existingItem.getStartTime());
        updatedItem.setEndTime(existingItem.getEndTime());
        updatedItem.setCurrentUserAutoBidActive(existingItem.isCurrentUserAutoBidActive());
        updatedItem.setCurrentUserAutoBidMaxBid(existingItem.getCurrentUserAutoBidMaxBid());
        updatedItem.setCurrentUserAutoBidIncrementAmount(existingItem.getCurrentUserAutoBidIncrementAmount());
        existingItem.getBidHistory().forEach(updatedItem::addBid);

        boolean updated = itemDAO.updateItem(updatedItem);

        if (!updated) {
            throw new InvalidDataException("Cannot update item in database");
        }

        itemsById.put(updatedItem.getId(), updatedItem);
    }

    public synchronized void deleteItem(String itemId) throws ItemNotFoundException {
        Item item = findItemById(itemId);

        boolean deleted = itemDAO.deleteItem(item.getId());

        if (!deleted) {
            throw new ItemNotFoundException("Cannot delete item from database");
        }

        itemsById.remove(item.getId());
    }

    public synchronized Item findItemById(String itemId) throws ItemNotFoundException {
        if (isBlank(itemId)) {
            throw new ItemNotFoundException("Item not found");
        }

        Item item = itemsById.get(itemId);

        if (item == null) {
            item = itemDAO.findById(itemId);

            if (item != null) {
                itemsById.put(item.getId(), item);
            }
        }

        if (item == null) {
            throw new ItemNotFoundException("Item not found");
        }

        return item;
    }

    public synchronized List<Item> getAllItems() {
        List<Item> items = new ArrayList<>(itemsById.values());
        items.sort(Comparator.comparing(Item::getId, Comparator.nullsLast(String::compareTo)));
        return items;
    }

    synchronized void resetForTest() {
        itemsById.clear();
    }

    private void validateItemForCreate(Item item) throws InvalidDataException {
        if (item == null) {
            throw new InvalidDataException("Item must not be null");
        }
        if (isBlank(item.getId())) {
            throw new InvalidDataException("Item id must not be blank");
        }
        if (itemsById.containsKey(item.getId()) || itemDAO.findById(item.getId()) != null) {
            throw new InvalidDataException("Item id already exists");
        }
        if (isBlank(item.getName())) {
            throw new InvalidDataException("Item name must not be blank");
        }
        if (isBlank(item.getDescription())) {
            throw new InvalidDataException("Item description must not be blank");
        }
        if (item.getStartPrice() < 0) {
            throw new InvalidDataException("Start price must be greater than or equal to 0");
        }
    }

    private void requireExistingItemId(String itemId) throws ItemNotFoundException {
        findItemById(itemId);
    }

    private void seedSampleItems() {
        Item item1 = new Item("ITEM-1", "Laptop Gaming", "Laptop choi game, card RTX", 1500, 1500, AuctionStatus.OPEN);
        Item item2 = new Item("ITEM-2", "Dien thoai thong minh", "Flagship 256GB", 900, 900, AuctionStatus.OPEN);
        itemsById.put(item1.getId(), item1);
        itemsById.put(item2.getId(), item2);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
