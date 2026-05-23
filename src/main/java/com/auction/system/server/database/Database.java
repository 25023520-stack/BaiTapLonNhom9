package com.auction.system.server.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Database {
    private static final Logger LOGGER = LoggerFactory.getLogger(Database.class);
    private static final String MYSQL_DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";

    // create separate sql environment for users

    private static final String  DB_HOST =  System.getenv().getOrDefault("DB_HOST", "localhost" ) ;
    private static final String DB_PORT = System.getenv().getOrDefault("DB_PORT", "3306");
    private static final String DB_NAME = System.getenv().getOrDefault("DB_NAME", "auction_db");
    private static final String APP_TIMEZONE = System.getenv().getOrDefault("APP_TIMEZONE", "Asia/Bangkok");
    private static final String DB_URL =
            "jdbc:mysql://" + DB_HOST + ":" +DB_PORT + "/" + DB_NAME
            + "?useSSL=false"
            + "&allowPublicKeyRetrieval=true"
            + "&useUnicode=true"
            + "&characterEncoding=UTF-8"
            + "&serverTimezone=" + APP_TIMEZONE;

    private static final String DB_USER = System.getenv().getOrDefault("DB_USER", "auction_user");
    private static final String DB_PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "Auction123");
    private static final String ADMIN_BOOTSTRAP_ID = getEnvOrDefault("ADMIN_BOOTSTRAP_ID", "ADMIN-DEFAULT");
    private static final String ADMIN_BOOTSTRAP_FULL_NAME = getEnvOrDefault("ADMIN_BOOTSTRAP_FULL_NAME", "System Admin");
    private static final String ADMIN_BOOTSTRAP_USERNAME = getEnvOrDefault("ADMIN_BOOTSTRAP_USERNAME", "admin");
    private static final String ADMIN_BOOTSTRAP_EMAIL = getOptionalEnv("ADMIN_BOOTSTRAP_EMAIL");
    private static final String ADMIN_BOOTSTRAP_PASSWORD = getOptionalEnv("ADMIN_BOOTSTRAP_PASSWORD");

    private static final Database instance = new Database();

    private HikariDataSource dataSource;

    private Database() {
        loadDriver();
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

        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        // 0 = don't fail on startup if DB isn't ready yet; let callers retry
        config.setInitializationFailTimeout(0);

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
                    approved BOOLEAN NOT NULL DEFAULT TRUE,
                    balance DECIMAL(15,2) NOT NULL DEFAULT 0,
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
                    auction_approved BOOLEAN NOT NULL DEFAULT FALSE,
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

            String createAutoBidsTable = """
                    CREATE TABLE IF NOT EXISTS auto_bids (
                    id VARCHAR(100) PRIMARY KEY,
                    item_id VARCHAR(100) NOT NULL,
                    bidder_id VARCHAR(100) NOT NULL,
                    max_bid DECIMAL(15,2) NOT NULL,
                    increment_amount DECIMAL(15,2) NOT NULL,
                    active BOOLEAN NOT NULL DEFAULT TRUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_auto_bids_item
                        FOREIGN KEY (item_id) REFERENCES items(id),
                    CONSTRAINT fk_auto_bids_bidder
                        FOREIGN KEY (bidder_id) REFERENCES users(id),
                    CONSTRAINT uq_auto_bid_item_bidder
                        UNIQUE (item_id, bidder_id)
                    )
                    ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """;
            try(Statement stmt = connection1.createStatement()) {
                stmt.executeUpdate(createAutoBidsTable);
            }
          
            String createDepositRequestsTable = """
                    /* Bang luu yeu cau nap tien: bidder khong duoc tu cong tien,
                       admin phai duyet thi balance moi thay doi. */
                    CREATE TABLE IF NOT EXISTS deposit_requests (
                    id VARCHAR(100) PRIMARY KEY,
                    bidder_id VARCHAR(100) NOT NULL,
                    amount DECIMAL(15,2) NOT NULL,
                    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    reviewed_at TIMESTAMP NULL,
                    reviewed_by VARCHAR(100),
                    CONSTRAINT fk_deposit_bidder
                        FOREIGN KEY (bidder_id) REFERENCES users(id),
                    CONSTRAINT fk_deposit_admin
                        FOREIGN KEY (reviewed_by) REFERENCES users(id)
                    )
                    ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """;
            try(Statement stmt = connection1.createStatement()) {
                stmt.executeUpdate(createDepositRequestsTable);
            }
          
            ensureColumn(connection1, "users", "approved", "BOOLEAN NOT NULL DEFAULT TRUE");
            ensureColumn(connection1, "users", "balance", "DECIMAL(15,2) NOT NULL DEFAULT 0");
            ensureColumn(connection1, "items", "auction_approved", "BOOLEAN NOT NULL DEFAULT FALSE");
            backfillApprovalData(connection1);
            ensureDefaultAdmin(connection1);
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

    private void ensureColumn(Connection connection, String tableName, String columnName, String columnDefinition)
            throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet columns = metaData.getColumns(connection.getCatalog(), null, tableName, columnName)) {
            if (columns.next()) {
                return;
            }
        }

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition
            );
        }
    }

    private void backfillApprovalData(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    UPDATE users
                    SET approved = TRUE, balance = COALESCE(balance, 0)
                    WHERE approved IS NULL OR role IN ('ADMIN', 'BIDDER')
                    """);
            statement.executeUpdate("""
                    UPDATE items
                    SET auction_approved = TRUE
                    WHERE status IN ('RUNNING', 'FINISHED', 'PAID')
                    """);
        }
    }

    private void ensureDefaultAdmin(Connection connection) throws SQLException {
        if (isBlank(ADMIN_BOOTSTRAP_PASSWORD)) {
            LOGGER.info("Skipping bootstrap admin creation because ADMIN_BOOTSTRAP_PASSWORD is not set.");
            return;
        }
        String adminEmail = resolveBootstrapAdminEmail();

        String updateSql = """
                UPDATE users
                SET full_name = ?, username = ?, email = ?, password = ?, role = 'ADMIN', approved = TRUE, balance = 0
                WHERE username = ? OR id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
            statement.setString(1, ADMIN_BOOTSTRAP_FULL_NAME);
            statement.setString(2, ADMIN_BOOTSTRAP_USERNAME);
            statement.setString(3, adminEmail);
            statement.setString(4, ADMIN_BOOTSTRAP_PASSWORD);
            statement.setString(5, ADMIN_BOOTSTRAP_USERNAME);
            statement.setString(6, ADMIN_BOOTSTRAP_ID);
            if (statement.executeUpdate() > 0) {
                LOGGER.info("Bootstrap admin account synchronized from environment.");
                return;
            }
        }

        String insertSql = """
                INSERT INTO users (id, full_name, username, email, password, role, approved, balance)
                SELECT ?, ?, ?, ?, ?, ?, ?, ?
                WHERE NOT EXISTS (
                    SELECT 1 FROM users WHERE username = ?
                )
                """;

        try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
            statement.setString(1, ADMIN_BOOTSTRAP_ID);
            statement.setString(2, ADMIN_BOOTSTRAP_FULL_NAME);
            statement.setString(3, ADMIN_BOOTSTRAP_USERNAME);
            statement.setString(4, adminEmail);
            statement.setString(5, ADMIN_BOOTSTRAP_PASSWORD);
            statement.setString(6, "ADMIN");
            statement.setBoolean(7, true);
            statement.setBigDecimal(8, java.math.BigDecimal.ZERO);
            statement.setString(9, ADMIN_BOOTSTRAP_USERNAME);
            statement.executeUpdate();
        }
    }

    private static String getEnvOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return isBlank(value) ? defaultValue : value.trim();
    }

    private static String getOptionalEnv(String name) {
        String value = System.getenv(name);
        return isBlank(value) ? null : value.trim();
    }

    private static String resolveBootstrapAdminEmail() {
        if (!isBlank(ADMIN_BOOTSTRAP_EMAIL)) {
            return ADMIN_BOOTSTRAP_EMAIL;
        }
        String safeUsername = isBlank(ADMIN_BOOTSTRAP_USERNAME) ? "admin" : ADMIN_BOOTSTRAP_USERNAME.trim().toLowerCase();
        return safeUsername + "@bootstrap.local";
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}
