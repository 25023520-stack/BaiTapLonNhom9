package com.auction.system.server.database;

import java.sql.Connection;

public class TestDatabase {
    public static void main(String[] args) {
        try (Connection conn = Database.getInstance().getConnection()) {
            System.out.println("Ket noi MySQL thanh cong!");
        } catch (Exception e) {
            System.err.println("Ket noi MySQL that bai!");
            e.printStackTrace();
        }

        Database.getInstance().initializeDatabase();
        System.out.println("da goi initializaDatabase");
    }
}
