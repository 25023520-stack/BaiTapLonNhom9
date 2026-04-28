package com.auction.system.model.item;

public class Vehicle extends Item {
    public Vehicle(String id, String name, String description, double startingPrice, String sellerId) {
        super(id, name, description, startingPrice, sellerId);
    }

    public String getCategory() {
        return "VEHICLE";
    }   
}
