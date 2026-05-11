package com.auction.system.model.user;

import com.auction.system.model.item.Item;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

public class Seller extends User {
    private final List<Item> itemForSale = new ArrayList<>();

    public Seller(int id, String fullName, String username, String email, String passWord) {
        super();
    }

    public Seller(String id, String fullName, String userName,String email, String passWord) {
        super(id, fullName, userName, email, passWord);

    }

    public String getRole() {
        return "SELLER";
    }

    // lấy danh sách item mà seller đang bán
    public List<Item> getItemForSale() {
        return itemForSale;
    }
/* thêm item vào danh sách itemForSale của seller
* ném IllegalArgumentException nếu item không tồn tại
* ném IllegalArgumentException nếu item không thuộc về seller
*/
    public void addItemForSale(Item item) {
    if (item == null) {
        throw new IllegalArgumentException("Item không tồn tại");
    } else if (!Objects.equals(item.getSellerId(), this.getId())) {
        throw new IllegalArgumentException("Item không thuộc về seller này");
    } else {
        itemForSale.add(item);
    }
}

    // xóa item khỏi danh sách itemForSale của seller
    public void removeItemForSale(Item item) {
        if (item == null) {
            throw new IllegalArgumentException("Item không tồn tại");
        }
        else if (!Objects.equals(item.getSellerId(), this.getId())) {
            throw new IllegalArgumentException("Item không thuộc về Seller này");
        }
        else {
            itemForSale.remove(item);
        }
    }

    // lấy danh sách item mà seller đang bán
    public List<Item> getItemsForSale() {
        return itemForSale;
    }

    public List<Item> getItems() {
        return new ArrayList<>(itemForSale);
    }

    public void updateItem(Item updatedItem) {
        for (int i = 0; i < itemForSale.size(); i++) {
            if (Objects.equals(itemForSale.get(i).getId(), updatedItem.getId())) {
                itemForSale.set(i, updatedItem);
                break;
            }
        }
        
    }
}
