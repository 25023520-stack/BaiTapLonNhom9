package com.auction.system.exception;

public class ItemNotFoundException extends Exception {  //khong tìm thấy sản phẩm
    public ItemNotFoundException(String message) {
        super(message);
    }
}
