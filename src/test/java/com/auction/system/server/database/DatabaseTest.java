package com.auction.system.server.database;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseTest {

    @Test
    void schemaCreatesRequiredTables() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:schema_test;MODE=MySQL", "sa", "")) {
            Database.executeSchema(connection);

            assertTrue(tableExists(connection, "USERS"));
            assertTrue(tableExists(connection, "ITEMS"));
            assertTrue(tableExists(connection, "AUCTIONS"));
            assertTrue(tableExists(connection, "BIDS"));
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet tables = metaData.getTables(null, null, tableName, new String[]{"TABLE"})) {
            return tables.next();
        }
    }
}
