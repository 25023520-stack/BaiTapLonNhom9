package com.auction.system.model.user;

public class Admin extends User {
    public Admin(int id, String fullName, String username, String email, String passWord) {}

    public Admin(String id, String fullName, String userName,String email, String passWord) {
        super(id, fullName, userName,email, passWord);
    }

    public String getRole() {
        return "ADMIN";
    }
}
