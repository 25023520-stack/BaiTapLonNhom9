package com.auction.system.server;

import com.auction.system.model.auction.AuctionStatus;
import com.auction.system.model.item.Item;
import com.auction.system.model.user.Bidder;
import com.auction.system.model.user.Seller;
import com.auction.system.model.user.User;
import com.auction.system.server.manager.AuctionManager;
import com.auction.system.server.network.AuctionServer;

import java.io.IOException;
import java.net.BindException;
import java.time.LocalDateTime;

public class ServerMain {
    private static final int DEFAULT_PORT = 5050;
    private static final AuctionManager AUCTION_MANAGER = AuctionManager.getInstance();
    private static Thread serverThread;
    private static boolean demoDataSeeded;

    public static void main(String[] args) throws Exception {
        ensureDemoData();
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

        Seller seller1 = new Seller("S1", "Nguyen Van Seller", "seller1", "123456");
        Seller seller2 = new Seller("S2", "Tran Thi Seller", "seller2", "123456");
        Bidder bidder1 = new Bidder("B1", "Pham Minh An", "bidder1", "123456");
        Bidder bidder2 = new Bidder("B2", "Le Thu Ha", "bidder2", "123456");
        Bidder bidder3 = new Bidder("B3", "Do Quang Huy", "bidder3", "123456");

        registerIfAbsent(seller1);
        registerIfAbsent(seller2);
        registerIfAbsent(bidder1);
        registerIfAbsent(bidder2);
        registerIfAbsent(bidder3);

        if (AUCTION_MANAGER.findItemById("1").isEmpty()) {
            Item laptop = new Item("1", "Laptop Gaming", "Laptop RTX 4060, RAM 16GB, SSD 1TB.", 15000000, 0, AuctionStatus.OPEN);
            Item phone = new Item("2", "IPhone 14", "May cu 99%, pin 90%, phu kien day du.", 11000000, 0, AuctionStatus.OPEN);
            Item camera = new Item("3", "May anh Sony", "Sony A6400 kem lens kit, hoat dong tot.", 13000000, 0, AuctionStatus.OPEN);

            AUCTION_MANAGER.addItem(laptop, seller1);
            AUCTION_MANAGER.addItem(phone, seller1);
            AUCTION_MANAGER.addItem(camera, seller2);

            LocalDateTime now = LocalDateTime.now();
            AUCTION_MANAGER.startAuction("1", now.minusHours(1), now.plusHours(6));
            AUCTION_MANAGER.startAuction("2", now.minusMinutes(30), now.plusHours(4));
            AUCTION_MANAGER.startAuction("3", now.minusMinutes(15), now.plusHours(2));

            AUCTION_MANAGER.placeBid("1", bidder1, 16000000);
            AUCTION_MANAGER.placeBid("1", bidder2, 16800000);
            AUCTION_MANAGER.placeBid("2", bidder3, 11800000);
        }

        demoDataSeeded = true;
    }

    private static void registerIfAbsent(User user) {
        try {
            AUCTION_MANAGER.registerUser(user);
        } catch (IllegalArgumentException ignored) {
            // The singleton managers may already contain demo accounts if another local server already seeded them.
        }
    }
}
