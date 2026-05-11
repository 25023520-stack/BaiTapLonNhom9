package com.auction.system.model.user;

public class Bidder extends User {

    public Bidder(int id, String fullName, String username, String email, String passWord) {
    }

    public Bidder(String id, String fullName, String userName,String email, String passWord) {
        super(id, fullName, userName,email, passWord);
    }

    public String getRole() {
        return "BIDDER";
    }

}
