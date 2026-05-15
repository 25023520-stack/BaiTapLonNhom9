package com.auction.system.server.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Database {
    private static final Logger LOGGER = LoggerFactory.getLogger(Database.class);
    private static final String MYSQL_DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";

    private static final String DB_URL =
            "jdbc:mysql://26.207.130.115:3306/auction_db"
            +"?useSSL=false"
            + "&allowPublicKeyRetrieval=true"
            +"&serverTimezone=Asia/Bangkok"
            +"&characterEncoding=utf8"
            ;

    private static final String DB_USER = "auction_user";
    private static final String DB_PASSWORD = "Auction@123456";

    private static final Database instance = new Database();

    private Database() {
        loadDriver();
    }
//Singleton
    public static synchronized Database getInstance() {
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

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
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP 
                        )
                    ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """;
            try(Statement stmt = connection1.createStatement()) {
                stmt.executeUpdate(createUsersTable);
            }
            String createItemsTable = """
                    CREATE TABLE IF NOT EXISTS items (
                    id VARCHAR(100) PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    description TEXT,
                    image_path VARCHAR(500),
                    start_price DECIMAL(15,2) NOT NULL,
                    current_price DECIMAL(15,2) NOT NULL,
                    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
                    seller_id VARCHAR(100) NOT NULL,
                    highest_bidder_id VARCHAR(100),
                    start_time TIMESTAMP NULL,
                    end_time  TIMESTAMP NULL,
                    CONSTRAINT fk_items_seller
                        FOREIGN KEY (seller_id) REFERENCES users(id),
                    CONSTRAINT fk_items_highest_bidder
                        FOREIGN KEY (highest_bidder_id) REFERENCES users(id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
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
                    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK ( status IN ('OPEN', 'RUNNING', 'FINISHED', 'PAID', 'CANCELED')),
                    winner_id VARCHAR(100),
                    final_price DECIMAL(15,2) NOT NULL DEFAULT 0,
                    CONSTRAINT fk_auctions_item
                        FOREIGN KEY (item_id) REFERENCES items(id),
                    CONSTRAINT fk_auctions_winner
                        FOREIGN KEY (winner_id) REFERENCES users(id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
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
                    amount DECIMAL(15,2) NOT NULL,
                    bid_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_bids_auction
                        FOREIGN KEY (auction_id) REFERENCES auctions(id),
                    CONSTRAINT fk_bids_item
                        FOREIGN KEY (item_id) REFERENCES items(id),
                    CONSTRAINT fk_bids_bidder
                        FOREIGN KEY (bidder_id) REFERENCES users(id)
                    )
                    ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """;
            try(Statement stmt = connection1.createStatement()) {
                stmt.executeUpdate(createBidsTable);
            }
            LOGGER.info("Khoi tao database hoan tat.");
        }
        catch (SQLException e) {
            LOGGER.error("Loi khi tao bang database.", e);
        }
    }

    private void loadDriver() {
        try {
            Class.forName(MYSQL_DRIVER_CLASS);
            LOGGER.info("MySQL JDBC driver loaded.");
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Khong tim thay MySQL JDBC driver trong runtime classpath.", exception);
        }
    }



}
