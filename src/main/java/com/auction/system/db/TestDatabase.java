package com.auction.system.db;

import com.auction.system.db.Database;

public class TestDatabase {
    public static void main(String[] args) {
        Database.getInstance().initializeDatabase();
    }
}
