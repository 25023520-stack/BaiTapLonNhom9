package com.auction.system.server.manager;

import com.auction.system.exception.InvalidDataException;
import com.auction.system.exception.ItemNotFoundException;
import com.auction.system.model.auction.AuctionStatus;
import com.auction.system.model.item.Item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemManager {
    private static final ItemManager INSTANCE = new ItemManager(false);

    private final Map<String, Item> itemsById = new HashMap<>();

    private ItemManager() {
        this(false);
    }

    ItemManager(boolean seedSampleItems) {
        if (seedSampleItems) {
            seedSampleItems();
        }
    }

    public static ItemManager getInstance() {
        return INSTANCE;
    }

    public synchronized void addItem(Item item) throws InvalidDataException {
        validateItemForCreate(item);
        itemsById.put(item.getId(), item);
    }

    public synchronized void updateItem(String itemId, String name, String description, double startPrice)
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

        existingItem.setName(name.trim());
        existingItem.setDescription(description.trim());
        existingItem.setStartPrice(startPrice);
        existingItem.setCurrentPrice(startPrice);
    }

    public synchronized void deleteItem(String itemId) throws ItemNotFoundException {
        requireExistingItemId(itemId);
        itemsById.remove(itemId);
    }

    public synchronized Item findItemById(String itemId) throws ItemNotFoundException {
        requireExistingItemId(itemId);
        return itemsById.get(itemId.trim());
    }

    public synchronized List<Item> getAllItems() {
        List<Item> items = new ArrayList<>(itemsById.values());
        items.sort(Comparator.comparing(Item::getId));
        return items;
    }

    private void validateItemForCreate(Item item) throws InvalidDataException {
        if (item == null) {
            throw new InvalidDataException("Item must not be null");
        }
        if (isBlank(item.getId())) {
            throw new InvalidDataException("Item id must not be blank");
        }
        if (itemsById.containsKey(item.getId().trim())) {
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
        if (isBlank(itemId) || !itemsById.containsKey(itemId.trim())) {
            throw new ItemNotFoundException("Item not found");
        }
    }

    private void seedSampleItems() {
        Item item1 = new Item("1", "Laptop Gaming", "Laptop choi game, card RTX", 1500, 1500, AuctionStatus.OPEN);
        Item item2 = new Item("2", "Dien thoai thong minh", "Flagship 256GB", 900, 900, AuctionStatus.OPEN);
        itemsById.put(item1.getId(), item1);
        itemsById.put(item2.getId(), item2);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
