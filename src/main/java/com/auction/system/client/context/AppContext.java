package com.auction.system.client.context;

import com.auction.system.client.network.AuctionClient;
import com.auction.system.common.payload.Payload;
import com.auction.system.common.payload.PayloadType;
import com.auction.system.model.user.User;
import com.auction.system.model.user.Admin;
import com.auction.system.model.user.Bidder;
import com.auction.system.model.user.Seller;
import java.util.Map;

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

    public static synchronized void logout() throws IOException {
        IOException logoutException = null;
        try {
            if (AUCTION_CLIENT.isConnected()) {
                AUCTION_CLIENT.send(new Payload(PayloadType.DISCONNECT));
            }
        } catch (IOException exception) {
            logoutException = exception;
        } finally {
            currentUser = null;
            try {
                AUCTION_CLIENT.close();
            } catch (IOException exception) {
                if (logoutException == null) {
                    logoutException = exception;
                } else {
                    logoutException.addSuppressed(exception);
                }
            }
        }

        if (logoutException != null) {
            throw logoutException;
        }
    }

    public static synchronized void setCurrentUserFromMap(Map<String, Object> userMap) {
        if (userMap == null) return;
        String id = getString(userMap, "id");
        String fullName = getString(userMap, "fullName");
        String username = getString(userMap, "userName");
        String email = getString(userMap, "email");
        String role = getString(userMap, "role");
        currentUser = switch (role.toUpperCase()) {
            case "ADMIN"  -> new Admin(id, fullName, username, email, "");
            case "SELLER" -> new Seller(id, fullName, username, email, "");
            default       -> new Bidder(id, fullName, username, email, "");
        };
    }

    private static String getString(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? String.valueOf(v) : "";
    }

    private static void ensureConnected() throws IOException {
        if (AUCTION_CLIENT.isConnected()) {
            return;
        }

        AUCTION_CLIENT.connect(DEFAULT_HOST, DEFAULT_PORT);
    }
}
