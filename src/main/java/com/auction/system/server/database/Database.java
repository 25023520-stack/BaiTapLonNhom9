package com.auction.system.server.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Database {
    private static final Logger LOGGER = LoggerFactory.getLogger(Database.class);

    private static final String DB_HOST = "26.207.130.115";
    private static final String DB_PORT = "3306";
    private static final String DB_NAME = "auction_db";

    private static final String DB_URL =
            "jdbc:mysql://" + DB_HOST + ":" +DB_PORT + "/" + DB_NAME
            + "?useSSL=false"
            + "&allowPublicKeyRetrieval=true"
            + "&useUnicode=true"
            + "&characterEncoding=UTF-8"
            + "&serverTimezone=Asia/Bangkok";

    private static final String DB_USER = "auction_user";
    private static final String DB_PASSWORD = "Auction@123456";

    private static final Database instance = new Database();

    private HikariDataSource dataSource;

    private Database() {
        initializeConnectionPool();
    }
//Singleton
    public static synchronized Database getInstance() {return instance;
    }

    //khoi tao pool
    private void initializeConnectionPool() {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(DB_URL);
        config.setUsername(DB_USER);
        config.setPassword(DB_PASSWORD);

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(3);

        config.setConnectionTimeout(10000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        config.setPoolName("AuctionSystemPool");

        dataSource = new HikariDataSource(config);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
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



}
