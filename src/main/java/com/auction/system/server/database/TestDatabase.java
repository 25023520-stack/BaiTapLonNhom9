package com.auction.system.server.database;

public class TestDatabase {
    public static void main(String[] args) {
        Database.getInstance().initializeDatabase();
    }
}
