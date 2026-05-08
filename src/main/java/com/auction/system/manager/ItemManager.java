package com.auction.system.manager;

import com.auction.system.exception.InvalidDataException;
import com.auction.system.exception.ItemNotFoundException;
import com.auction.system.model.item.Item;

import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

public class ItemManager {
    private static final ItemManager INSTANCE = new ItemManager();

    private final Map<Integer, Item> items = new HashMap<>();

    private ItemManager() {
    }

    ItemManager(boolean ignored) {
    }

    public static ItemManager getInstance() {
        return INSTANCE;
    }

    public void addItem(Item item) throws InvalidDataException {
        if (item == null) {
            throw new InvalidDataException("Sản phẩm không được để trống");
        }
        if (item.getName() == null || item.getName().isBlank()) {
            throw new InvalidDataException("Tên sản phẩm không được để trống");
        }
        if (item.getStartPrice() <= 0) {
            throw new InvalidDataException("Giá khởi điểm phải lớn hơn 0");
        }

        items.put(item.getId(), item);
    }

    public Item findItemById(int itemId) throws ItemNotFoundException {
        Item item = items.get(itemId);
        if(item == null) {
            throw new ItemNotFoundException("Không tìm thấy sản phẩm");
        }

        return item;
    }

    public void updateItem(int itemId, String newName, String newDescription, double newPrice) throws ItemNotFoundException, InvalidDataException  {
        if (newName == null || newName.isBlank()) {
            throw new InvalidDataException("Tên sản phẩm không được để trống");
        }

        if (newPrice <= 0) {
            throw new InvalidDataException("Giá khởi điểm phải lớn hơn 0");
        }

        Item item = findItemById(itemId);

        item.setName(newName);
        item.setCurrentPrice(newPrice);
        item.setDescription(newDescription);
    }

    public void deleteItem(int itemId) throws ItemNotFoundException {
        findItemById(itemId);
        items.remove(itemId);
    }

    public Collection<Item> getAllItems() {
        return items.values();
    }
    
}
