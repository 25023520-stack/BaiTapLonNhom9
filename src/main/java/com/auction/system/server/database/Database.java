package com.auction.system.server.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private static final String DB_URL = "jdbc:h2:file:./data/auction";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    private static final Database instance = new Database();
    private static Connection connection;

    private Database() {}
//Singleton
    public static synchronized Database getInstance() {
        return instance;
    }

    public Connection getConnection() throws SQLException {
        if(connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        }
        return connection;
    }

    // Khá»Ÿi táº¡o DB
    public void initializeDatabase() {
        try (Connection connection1 = getConnection()) {
            String createUsersTable = """
                    CREATE TABLE IF NOT EXISTS users (
                    id VARCHAR(100) PRIMARY KEY,
                    full_name VARCHAR(255) NOT NULL,
                    username VARCHAR(50) UNIQUE NOT NULL,
                    email VARCHAR(100) UNIQUE NOT NULL,
                    password VARCHAR(255) NOT NULL,
                    role VARCHAR(20) DEFAULT 'BIDDER',
                    create_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP )
                    """;
            try(Statement stmt = connection1.createStatement()) {
                stmt.executeUpdate(createUsersTable);
            }
            String createItemsTable = """
                    CREATE TABLE IF NOT EXISTS items (
                    id VARCHAR(100) PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    description CLOB,
                    image_path VARCHAR(500),
                    start_price DOUBLE NOT NULL,
                    current_price DOUBLE NOT NULL,
                    STATUS VARCHAR(30) NOT NULL,
                    seller_id VARCHAR(100) NOT NULL,
                    highest_bidder_id VARCHAR(100),
                    start_time TIMESTAMP NOT NULL,
                    end_time  TIMESTAMP NOT NULL )
                    """;
            try(Statement stmt = connection1.createStatement()) {
                stmt.executeUpdate(createItemsTable);
            }
            String createAuctionsTable = """
                    CREATE TABLE IF NOT EXISTS auctions (
                    id VARCHAR(100) PRIMARY KEY,
                    item_id VARCHAR(100) UNIQUE NOT NULL,
                    start_time TIMESTAMP NOT NULL,
                    end_time TIMESTAMP NOT NULL,
                    status VARCHAR(20) NOT NULL CHECK ( status IN ('OPEN', 'RUNNING', 'FINISHED', 'PAID', 'CANCELED')),
                    winner_id VARCHAR(100),
                    final_price DOUBLE DEFAULT 0,
                    FOREIGN KEY (item_id) REFERENCES items(id),
                    FOREIGN KEY (winner_id) REFERENCES users(id) )
                    """;
            try(Statement stmt = connection1.createStatement()) {
                stmt.executeUpdate(createAuctionsTable);
            }
            String createBidsTable = """
                    CREATE TABLE IF NOT EXISTS bids (
                    id VARCHAR(100) PRIMARY KEY,
                    auction_id VARCHAR(100) NOT NULL,
                    item_id VARCHAR(100) NOT NULL,
                    bidder_id VARCHAR(100) NOT NULL,
                    amount DOUBLE NOT NULL,
                    bid_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (auction_id) REFERENCES auctions(id),
                    FOREIGN KEY (item_id) REFERENCES items(id),
                    FOREIGN KEY (bidder_id) REFERENCES users(id)
                    )
                    """;
            try(Statement stmt = connection1.createStatement()) {
                stmt.executeUpdate(createBidsTable);
            }
            System.out.println("=== khoi tao database hoan tat ===");
        }
        catch (SQLException e) {
            System.err.println("Lỗi khi tạo bảng " + e.getMessage());
            e.printStackTrace();
        }
    }



}
