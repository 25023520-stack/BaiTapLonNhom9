package com.auction.system.server;

import com.auction.system.server.database.Database;
import com.auction.system.server.manager.AuctionManager;
import com.auction.system.server.manager.ItemManager;
import com.auction.system.server.network.AuctionServer;
import com.auction.system.server.scheduler.AuctionScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ServerMain {
    private static final int DEFAULT_PORT = 5050;
    private static final AuctionManager AUCTION_MANAGER = AuctionManager.getInstance();
    private static Thread serverThread;
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerMain.class);

    public static void main(String[] args) throws Exception {
        waitForDatabase();
        Database.getInstance().initializeDatabase();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Database.getInstance().shutdown();
        }));

        ItemManager.getInstance().loadItemsFromDatabase();
        purgeLegacyDemoItems();
        logServerAddress(DEFAULT_PORT);
        AuctionServer server = new AuctionServer(DEFAULT_PORT, AUCTION_MANAGER);
        AUCTION_MANAGER.setAuctionSubject(server);

        AuctionScheduler scheduler = new AuctionScheduler(AUCTION_MANAGER);
        scheduler.start();

        server.start();
    }

    public static synchronized void startInBackground() throws IOException {
        if (serverThread != null && serverThread.isAlive()) {
            return;
        }

        Database.getInstance().initializeDatabase();
        ItemManager.getInstance().loadItemsFromDatabase();
        purgeLegacyDemoItems();
        AuctionServer server = new AuctionServer(DEFAULT_PORT, AUCTION_MANAGER);
        AUCTION_MANAGER.setAuctionSubject(server);
        new AuctionScheduler(AUCTION_MANAGER).start();
        serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (BindException ignored) {
                // Another server instance is already running on the configured port.
            } catch (IOException exception) {
                throw new RuntimeException("Cannot start auction server", exception);
            }
        }, "auction-server");
        serverThread.setDaemon(true);
        serverThread.start();

        try {
            Thread.sleep(150);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for server startup", exception);
        }
    }

    private static void waitForDatabase() {
        int maxAttempts = 20;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try (Connection conn = Database.getInstance().getConnection()) {
                LOGGER.info("Database connection established (attempt {}).", attempt);
                return;
            } catch (SQLException e) {
                LOGGER.warn("Database not ready yet (attempt {}/{}), retrying in 3s...", attempt, maxAttempts);
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for database", ie);
                }
            }
        }
        throw new RuntimeException("Database did not become ready after " + maxAttempts + " attempts.");
    }

    private static void purgeLegacyDemoItems() {
        String[] ids = {"ITEM-1", "ITEM-2", "ITEM-3"};
        String placeholders = "('ITEM-1','ITEM-2','ITEM-3')";
        try (Connection conn = Database.getInstance().getConnection()) {
            try (PreparedStatement s = conn.prepareStatement(
                    "DELETE FROM bids WHERE item_id IN " + placeholders)) {
                s.executeUpdate();
            }
            try (PreparedStatement s = conn.prepareStatement(
                    "DELETE FROM auctions WHERE item_id IN " + placeholders)) {
                s.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.warn("Could not purge demo item dependents: {}", e.getMessage());
            return;
        }
        for (String id : ids) {
            try {
                ItemManager.getInstance().deleteItem(id);
            } catch (Exception ignored) {
                // Item may not exist if already purged on a previous run
            }
        }
    }

    private static void logServerAddress(int port) {
        try {
            // lấy IP thật của máy trên mạng WiFi hiện tại
            InetAddress localHost = InetAddress.getLocalHost();
            String ip = localHost.getHostAddress();
            LOGGER.info("=================================");
            LOGGER.info("Server started!");
            LOGGER.info("Local IP : {}", ip);
            LOGGER.info("Port     : {}", port);
            LOGGER.info("=================================");
        } catch (UnknownHostException e) {
            System.out.println("Server started on port " + port + " (could not determine IP)");
        }
    }
}
