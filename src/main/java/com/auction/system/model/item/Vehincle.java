package com.auction.system.model.item;

public class Vehincle extends Item {
    public Vehincle(String id, String name, String description, double startingPrice, String sellerId) {
        super(id, name, description, startingPrice, sellerId);
    }

    public String getCategory() {
        return "VEHINCLE";
    }   
}
