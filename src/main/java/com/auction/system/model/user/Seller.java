package com.auction.system.model;

public class Seller extends User {

    public Seller() {}

    public Seller(String id, String fullName, String userName, String passWord) {
        super(id, fullName, userName, passWord);
    }

    public String getRole() {
        return "SELLER";
    }
}
