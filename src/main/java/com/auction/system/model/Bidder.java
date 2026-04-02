package com.auction.system.model;

public class Bidder extends User {

    public Bidder() {
    }

    public Bidder(String id, String fullName, String userName, String passWord) {
        super(id, fullName, userName, passWord);
    }

    public String getRole() {
        return "BIDDER"
    }

}
