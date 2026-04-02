package com.auction.system.model;

public class Admin extends User {
    public Admin() {}

    public Admin(String id, String fullName, String userName, String passWord) {
        super(id, fullName, userName, passWord);
    }

    public String getRole() {
        return "ADMIN";
    }
}
