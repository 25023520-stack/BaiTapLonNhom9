package com.auction.system.common.payload;

public enum PayloadType {
    LOGIN,
    REGISTER,
    LIST_ITEMS,
    LIST_ITEMS_BY_SELLER,
    ADD_ITEM,
    UPDATE_ITEM,
    REMOVE_ITEM,
    START_AUCTION,
    BID,
    AUTO_BID_SET,
    AUTO_BID_CANCEL,
    ADMIN_DASHBOARD,
    APPROVE_SELLER,
    APPROVE_AUCTION,
    RESPONSE,
    UPDATE_AUCTION,
    DISCONNECT
}