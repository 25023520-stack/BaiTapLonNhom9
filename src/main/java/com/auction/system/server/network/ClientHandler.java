package com.auction.system.server.network;

import com.auction.system.common.json.GsonProvider;
import com.auction.system.common.payload.BidPayload;
import com.auction.system.common.payload.Payload;
import com.auction.system.common.payload.PayloadType;
import com.auction.system.common.payload.ResponsePayload;
import com.auction.system.server.controller.AdminController;
import com.auction.system.server.controller.AuctionController;
import com.auction.system.server.controller.AuthController;
import com.auction.system.server.manager.AuctionManager;
import com.auction.system.model.user.User;
import com.auction.system.model.user.Bidder;
import com.auction.system.server.observer.AuctionObserver;
import com.auction.system.model.auction.AutoBid;
import com.auction.system.model.item.Item;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class ClientHandler implements Runnable, Closeable, AuctionObserver {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientHandler.class);
    private static final Gson GSON = GsonProvider.get();

    private final Socket socket;
    private final AuctionManager auctionManager;
    private final AuctionServer auctionServer;
    private final AuctionController auctionController;
    private final AdminController adminController;
    private final AuthController authController;
    private final PrintWriter writer;
    private final BufferedReader reader;

    private volatile boolean connected = true;
    private volatile boolean closed;
    private User authenticatedUser;


    public ClientHandler(Socket socket, AuctionManager auctionManager, AuctionServer auctionServer) throws IOException {
        this.socket = socket;
        this.auctionManager = auctionManager;
        this.auctionServer = auctionServer;
        this.auctionController = new AuctionController();
        this.adminController = new AdminController();
        this.authController = new AuthController();
        this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.auctionServer.addObserver(this);
    }

    @Override
    public void run() {
        LOGGER.info("Client connected: {}", socket.getInetAddress());
        try {
            String line;
            while (connected && (line = reader.readLine()) != null) {
                try {
                    Payload payload = GSON.fromJson(line, Payload.class);
                    handle(payload);
                } catch (RuntimeException exception) {
                    // Ghi chu: moi request tu client phai duoc co lap loi.
                    // Neu mot request bid/payload loi ma de exception thoat khoi loop nay,
                    // socket se bi dong va client se bi day sang man "May chu khong kha dung".
                    LOGGER.error("Failed to handle client payload: {}", exception.getMessage(), exception);
                    send(ResponsePayload.error("Server cannot process this request: " + exception.getMessage()));
                }
            }
        } catch (IOException e) {
            if (connected) {
                LOGGER.warn("Connection error: {}", e.getMessage());
            }
        } finally {
            try {
                close();
            } catch (IOException ignored) {
            }
        }
    }

    private void handle(Payload payload) throws IOException {
        if (payload == null) {
            send(ResponsePayload.error("Payload is required"));
            return;
        }

        PayloadType type = payload.getType();
        if (type == null) {
            send(ResponsePayload.error("Payload type is required"));
            return;
        }


        try {
            switch (type) {
                case LOGIN -> handleLogin(payload);
                case REGISTER -> handleRegister(payload);
                case LIST_ITEMS -> handleListItems();
                case LIST_ITEMS_BY_SELLER -> send(auctionController.listItemsBySeller(payload, authenticatedUser));
                case ADD_ITEM -> handleItemMutation(payload, "ITEM_ADDED");
                case UPDATE_ITEM -> handleItemMutation(payload, "ITEM_UPDATED");
                case REMOVE_ITEM -> send(auctionController.removeItem(payload, authenticatedUser));
                case START_AUCTION -> handleItemMutation(payload, "AUCTION_APPROVAL_REQUESTED");
                case BID -> handleBid(payload);
                case AUTO_BID_SET -> handleAutoBidSet(payload);
                case AUTO_BID_CANCEL -> handleAutoBidCancel(payload);
                case ADMIN_DASHBOARD -> send(adminController.dashboard(authenticatedUser));
                case REQUEST_DEPOSIT -> send(adminController.requestDeposit(payload, authenticatedUser));
                case APPROVE_DEPOSIT -> send(adminController.approveDeposit(payload, authenticatedUser));
                case APPROVE_SELLER -> send(adminController.approveSeller(payload, authenticatedUser));
                case APPROVE_AUCTION -> handleAuctionApproval(payload);
                case MARK_AS_PAID -> handleMarkAsPaid(payload);
                case DECLINE_WIN -> handleDeclineWin(payload);
                case DISCONNECT -> {
                    send(ResponsePayload.ok("Disconnected"));
                    close();
                }
                default -> send(ResponsePayload.error("Unsupported payload type: " + type));
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            send(ResponsePayload.error(e.getMessage()));
        } catch (RuntimeException e) {
            LOGGER.error("Unexpected server error while handling {}: {}", type, e.getMessage(), e);
            send(ResponsePayload.error("Server error while handling " + type + ": " + e.getMessage()));
        }
    }

    private void handleLogin(Payload payload) throws IOException {
        String username = payload.getString("username");
        String password = payload.getString("password");
        if (username == null || password == null) {
            send(ResponsePayload.error("Username and password are required"));
            return;
        }

        Optional<User> user = auctionManager.login(username, password);
        if (user.isEmpty()) {
            send(ResponsePayload.error("Invalid username or password"));
            return;
        }

        authenticatedUser = user.get();
        ResponsePayload response = ResponsePayload.ok("Login successful");
        response.put("id", authenticatedUser.getId());
        response.put("userName", authenticatedUser.getUserName());
        response.put("email", authenticatedUser.getEmail());
        response.put("role", authenticatedUser.getRole());
        response.put("fullName", authenticatedUser.getFullName());
        response.put("approved", authenticatedUser.isApproved());
        response.put("balance", authenticatedUser.getBalance());
        send(response);
        LOGGER.info("User logged in: {}", username);
    }

    private void handleRegister(Payload payload) throws IOException {
        ResponsePayload response = authController.register(payload);
        send(response);
        if (response.isSuccess()) {
            LOGGER.info("New user registered: {}", payload.getString("username"));
        }
    }

    private void handleListItems() throws IOException {
        send(auctionController.listItems(authenticatedUser));
    }

    private void handleItemMutation(Payload payload, String eventType) throws IOException {
        ResponsePayload response = switch (payload.getType()) {
            case ADD_ITEM -> auctionController.addItem(payload, authenticatedUser);
            case UPDATE_ITEM -> auctionController.updateItem(payload, authenticatedUser);
            case START_AUCTION -> auctionController.startAuction(payload, authenticatedUser);
            default -> ResponsePayload.error("Unsupported item mutation: " + payload.getType());
        };

        send(response);
        if (response.isSuccess()) {
            Object rawItem = response.getBody().get("item");
            if (rawItem instanceof Item item) {
                auctionServer.notifyObservers(item, eventType);
            }
        }
    }

    private void handleBid(Payload payload) throws IOException {
        if (!(authenticatedUser instanceof Bidder bidder)) {
            send(ResponsePayload.error("Please login with a bidder account before placing a bid"));
            return;
        }
        ResponsePayload response = auctionController.placeBid(payload, authenticatedUser);
        send(response);

        if (response.isSuccess()) {
            Object rawItem = response.getBody().get("item");
            if (rawItem instanceof Item updatedItem) {
                auctionServer.notifyObservers(updatedItem, "BID_PLACED");
            }
        }
    }

    private void handleAutoBidSet(Payload payload) throws IOException {
        if (!(authenticatedUser instanceof Bidder)) {
            send(ResponsePayload.error("Please login with a bidder account before enabling auto-bid"));
            return;
        }

        ResponsePayload response = auctionController.setAutoBid(payload, authenticatedUser);
        send(response);

        if (response.isSuccess()) {
            Object rawItem = response.getBody().get("item");
            if (rawItem instanceof Item updatedItem) {
                auctionServer.notifyObservers(updatedItem, "AUTO_BID_SET");
            }
        }
    }

    private void handleAutoBidCancel(Payload payload) throws IOException {
        if (!(authenticatedUser instanceof Bidder)) {
            send(ResponsePayload.error("Please login with a bidder account before canceling auto-bid"));
            return;
        }

        ResponsePayload response = auctionController.cancelAutoBid(payload, authenticatedUser);
        send(response);

        if (response.isSuccess()) {
            Object rawItem = response.getBody().get("item");
            if (rawItem instanceof Item updatedItem) {
                auctionServer.notifyObservers(updatedItem, "AUTO_BID_CANCEL");
            }
        }
    }

    private void handleMarkAsPaid(Payload payload) throws IOException {
        if (!(authenticatedUser instanceof Bidder)) {
            send(ResponsePayload.error("Only bidders can pay for won items"));
            return;
        }
        ResponsePayload response = auctionController.markAsPaid(payload, authenticatedUser);
        send(response);
        if (response.isSuccess()) {
            Object rawItem = response.getBody().get("item");
            if (rawItem instanceof Item item) {
                auctionServer.notifyObservers(item, "AUCTION_PAID");
            }
        }
    }

    private void handleDeclineWin(Payload payload) throws IOException {
        if (!(authenticatedUser instanceof Bidder)) {
            send(ResponsePayload.error("Only bidders can decline a win"));
            return;
        }
        ResponsePayload response = auctionController.declineWin(payload, authenticatedUser);
        send(response);
        if (response.isSuccess()) {
            Object rawItem = response.getBody().get("item");
            if (rawItem instanceof Item item) {
                auctionServer.notifyObservers(item, "AUCTION_CANCELED");
            }
        }
    }

    private void handleAuctionApproval(Payload payload) throws IOException {
        ResponsePayload response = adminController.approveAuction(payload, authenticatedUser);
        send(response);

        if (response.isSuccess()) {
            Object rawItem = response.getBody().get("item");
            boolean approved = response.getBody().get("approved") instanceof Boolean bool && bool;
            if (rawItem instanceof Item item) {
                auctionServer.notifyObservers(item, approved ? "AUCTION_STARTED" : "AUCTION_REQUEST_REJECTED");
            }
        }
    }

    @Override
    public void onAuctionUpdated(Item item, String eventType) {
        if (!connected || closed) return;

        attachAutoBidDataForCurrentUser(item);
        ResponsePayload update = ResponsePayload.auctionUpdate("Auction updated");
        update.put("itemId", item.getId());
        update.put("item", item);
        update.put("eventType", eventType);  // "BID_PLACED", "AUCTION_STARTED", "AUCTION_FINISHED"
        send(update);
    }

    private void attachAutoBidDataForCurrentUser(Item item) {
        if (item == null) {
            return;
        }

        item.setCurrentUserAutoBidActive(false);
        item.setCurrentUserAutoBidMaxBid(0);
        item.setCurrentUserAutoBidIncrementAmount(0);

        if (!(authenticatedUser instanceof Bidder bidder)) {
            return;
        }

        AutoBid autoBid = auctionManager.findActiveAutoBid(item.getId(), bidder.getId());
        if (autoBid == null) {
            return;
        }

        item.setCurrentUserAutoBidActive(true);
        item.setCurrentUserAutoBidMaxBid(autoBid.getMaxBid());
        item.setCurrentUserAutoBidIncrementAmount(autoBid.getIncrementAmount());
    }

    public synchronized void send(Payload payload){
        writer.println(GSON.toJson(payload));
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }

        closed = true;
        connected = false;
        auctionServer.removeClient(this);
        auctionServer.removeObserver(this);
        writer.close();
        reader.close();
        socket.close();
        LOGGER.info("Client disconnected: {}", socket.getInetAddress());
    }

}
