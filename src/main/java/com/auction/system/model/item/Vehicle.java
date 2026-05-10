package com.auction.system.model.item;

public class Vehicle extends Item {
    public Vehicle(int id, String name, String description, double startingPrice, int sellerId) {
        super(id, name, description, startingPrice, sellerId);
    }

    public String getCategory() {
        return "VEHICLE";
    }   
}
