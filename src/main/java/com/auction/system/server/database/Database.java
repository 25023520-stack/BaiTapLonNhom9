package com.auction.system.server.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

public class Database {
    private static final Logger LOGGER = LoggerFactory.getLogger(Database.class);
    private static final String SCHEMA_RESOURCE = "database/schema.sql";

    private static final String DB_URL =
            "jdbc:mysql://26.207.130.115:3306/auction_db"
                    + "?useSSL=false"
                    + "&allowPublicKeyRetrieval=true"
                    + "&serverTimezone=Asia/Bangkok"
                    + "&characterEncoding=utf8";

    private static final String DB_USER = "auction_user";
    private static final String DB_PASSWORD = "Auction@123456";

    private static final Database instance = new Database();

    private Database() {}
//Singleton
    public static synchronized Database getInstance() {
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public void initializeDatabase() {
        try (Connection connection = getConnection()) {
            executeSchema(connection);
            LOGGER.info("Khoi tao database hoan tat.");
        } catch (SQLException | IOException e) {
            LOGGER.error("Loi khi tao bang database.", e);
        }
    }

    static void executeSchema(Connection connection) throws SQLException, IOException {
        String schema = readSchema();
        try (Statement stmt = connection.createStatement()) {
            for (String statement : schema.split(";")) {
                String sql = statement.trim();
                if (!sql.isEmpty()) {
                    stmt.executeUpdate(sql);
                }
            }
        }
    }

    private static String readSchema() throws IOException {
        ClassLoader classLoader = Database.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (inputStream == null) {
                throw new IOException("Missing schema resource: " + SCHEMA_RESOURCE);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }
}
