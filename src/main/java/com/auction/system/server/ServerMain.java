package com.auction.system.server;

import com.auction.system.model.auction.AuctionStatus;
import com.auction.system.model.item.Item;
import com.auction.system.model.user.Bidder;
import com.auction.system.model.user.Seller;
import com.auction.system.model.user.User;
import com.auction.system.server.manager.AuctionManager;
import com.auction.system.server.network.AuctionServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;

public class ServerMain {
    private static final int DEFAULT_PORT = 5050;
    private static final AuctionManager AUCTION_MANAGER = AuctionManager.getInstance();
    private static Thread serverThread;
    private static boolean demoDataSeeded;
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerMain.class);

    public static void main(String[] args) throws Exception {
        ensureDemoData();
        logServerAddress(DEFAULT_PORT);
        AuctionServer server = new AuctionServer(DEFAULT_PORT, AUCTION_MANAGER);
        server.start();
    }

    public static synchronized void startInBackground() throws IOException {
        if (serverThread != null && serverThread.isAlive()) {
            return;
        }

        ensureDemoData();
        AuctionServer server = new AuctionServer(DEFAULT_PORT, AUCTION_MANAGER);
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

    private static synchronized void ensureDemoData() {
        if (demoDataSeeded) {
            return;
        }
        LOGGER.info("Seeding demo data into the system...");

        Seller seller1 = new Seller("SELLER-10", "Nguyen Van Seller","seller1","nguyenvanseller@example.com", "123456");
        Seller seller2 = new Seller("SELLER-11", "Tran Thi Seller", "seller2","seller2@example.com", "123456");
        Bidder bidder1 = new Bidder("BIDDER-12", "Pham Minh An", "bidder1","bidder1@example.com", "123456");
        Bidder bidder2 = new Bidder("BIDDER-13", "Le Thu Ha", "bidder2","bidder2@example.com", "123456");
        Bidder bidder3 = new Bidder("BIDDER-14", "Do Quang Huy", "bidder3","bidder3@exmaple.com", "123456");

        registerIfAbsent(seller1);
        registerIfAbsent(seller2);
        registerIfAbsent(bidder1);
        registerIfAbsent(bidder2);
        registerIfAbsent(bidder3);

        if (AUCTION_MANAGER.findItemById("ITEM-1").isEmpty()) {
            Item laptop = new Item("ITEM-1", "Laptop Gaming", "Laptop RTX 4060, RAM 16GB, SSD 1TB.", 15000000, 0, AuctionStatus.OPEN);
            Item phone = new Item("ITEM-2", "IPhone 14", "May cu 99%, pin 90%, phu kien day du.", 11000000, 0, AuctionStatus.OPEN);
            Item camera = new Item("ITEM-3", "May anh Sony", "Sony A6400 kem lens kit, hoat dong tot.", 13000000, 0, AuctionStatus.OPEN);

            AUCTION_MANAGER.addItem(laptop, seller1);
            AUCTION_MANAGER.addItem(phone, seller1);
            AUCTION_MANAGER.addItem(camera, seller2);

            LocalDateTime now = LocalDateTime.now();
            AUCTION_MANAGER.startAuction("ITEM-1", now.minusHours(1), now.plusHours(6));
            AUCTION_MANAGER.startAuction("ITEM-2", now.minusMinutes(30), now.plusHours(4));
            AUCTION_MANAGER.startAuction("ITEM-3", now.minusMinutes(15), now.plusHours(2));

            AUCTION_MANAGER.placeBid("ITEM-1", bidder1, 16000000);
            AUCTION_MANAGER.placeBid("ITEM-1", bidder2, 16800000);
            AUCTION_MANAGER.placeBid("ITEM-2", bidder3, 11800000);
        }
        LOGGER.info("Demo data seeded successfully.");
        demoDataSeeded = true;
    }

    private static void registerIfAbsent(User user) {
        try {
            AUCTION_MANAGER.registerUser(user);
        } catch (IllegalArgumentException ignored) {
            // The singleton managers may already contain demo accounts if another local server already seeded them.
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
