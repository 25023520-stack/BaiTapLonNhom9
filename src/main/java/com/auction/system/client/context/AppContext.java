package com.auction.system.client.context;

import com.auction.system.client.network.AuctionClient;
import com.auction.system.model.user.User;

import java.io.IOException;

public final class AppContext {
    private static final String DEFAULT_HOST = System.getProperty("auction.server.host", "127.0.0.1");
    private static final int DEFAULT_PORT = Integer.parseInt(System.getProperty("auction.server.port", "5050"));

    private static final AuctionClient AUCTION_CLIENT = new AuctionClient();
    private static User currentUser;

    private AppContext() {
    }

    public static synchronized AuctionClient getAuctionClient() throws IOException {
        ensureConnected();
        return AUCTION_CLIENT;
    }

    public static synchronized User getCurrentUser() {
        return currentUser;
    }

    public static synchronized void setCurrentUser(User user) {
        currentUser = user;
    }

    private static void ensureConnected() throws IOException {
        if (AUCTION_CLIENT.isConnected()) {
            return;
        }

        AUCTION_CLIENT.connect(DEFAULT_HOST, DEFAULT_PORT);
    }
}
