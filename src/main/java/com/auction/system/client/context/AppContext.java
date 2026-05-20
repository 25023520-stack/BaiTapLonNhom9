package com.auction.system.client.context;

import com.auction.system.client.network.AuctionClient;
import com.auction.system.common.payload.Payload;
import com.auction.system.common.payload.PayloadType;
import com.auction.system.model.user.User;
import com.auction.system.model.user.Admin;
import com.auction.system.model.user.Bidder;
import com.auction.system.model.user.Seller;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.Map;

import java.io.IOException;

public final class AppContext {
    private static final String DEFAULT_HOST = System.getProperty("auction.server.host", "127.0.0.1");
    private static final int DEFAULT_PORT = Integer.parseInt(System.getProperty("auction.server.port", "5050"));

    private static final AuctionClient AUCTION_CLIENT = new AuctionClient();
    private static User currentUser;
    private static String serverHost = DEFAULT_HOST;

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

    public static void goToServerDown(Stage stage) {
        currentUser = null;
        try { AUCTION_CLIENT.close(); } catch (IOException ignored) {}
        try {
            Parent view = FXMLLoader.load(
                    AppContext.class.getResource("/com/auction/system/client/view/ServerDown.fxml"));
            stage.setScene(new Scene(view, 900, 600));
            stage.setTitle("Máy chủ không khả dụng");
            stage.show();
        } catch (IOException ignored) {}
    }

    public static synchronized void setCurrentUserFromMap(Map<String, Object> userMap) {
        currentUser = mapToUser(userMap);
    }

    public static User mapToUser(Map<String, Object> userMap) {
        if (userMap == null) {
            return null;
        }

        String id = getString(userMap, "id");
        String fullName = getString(userMap, "fullName");
        String username = getString(userMap, "userName");
        String email = getString(userMap, "email");
        String role = getString(userMap, "role");
        boolean approved = getBoolean(userMap, "approved");

        User user = switch (role.toUpperCase()) {
            case "ADMIN"  -> new Admin(id, fullName, username, email, "");
            case "SELLER" -> new Seller(id, fullName, username, email, "");
            default       -> new Bidder(id, fullName, username, email, "");
        };
        user.setApproved(approved);
        return user;
    }

    private static String getString(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? String.valueOf(v) : "";
    }

    private static boolean getBoolean(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    public static synchronized void setServerHost(String host) {
        if (host != null && !host.isBlank()) {
            serverHost = host.trim();
        }
    }

    public static synchronized String getServerHost() {
        return serverHost;
    }

    private static void ensureConnected() throws IOException {
        if (AUCTION_CLIENT.isConnected()) {
            return;
        }

        AUCTION_CLIENT.connect(serverHost, DEFAULT_PORT);
    }
}
