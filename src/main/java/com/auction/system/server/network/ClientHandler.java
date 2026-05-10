package com.auction.system.server.network;

import com.auction.system.common.payload.BidPayload;
import com.auction.system.common.payload.Payload;
import com.auction.system.common.payload.PayloadType;
import com.auction.system.common.payload.ResponsePayload;
import com.auction.system.server.controller.AuctionController;
import com.auction.system.server.manager.AuctionManager;
import com.auction.system.model.user.User;
import com.auction.system.model.user.Bidder;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class ClientHandler implements Runnable, Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientHandler.class);
    private static final Gson GSON = new Gson();

    private final Socket socket;
    private final AuctionManager auctionManager;
    private final AuctionServer auctionServer;
    private final AuctionController auctionController;
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
        this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public void run() {
        LOGGER.info("Client connected: {}", socket.getInetAddress());
        try {
            String line;
            while (connected && (line = reader.readLine()) != null) {
                Payload payload = GSON.fromJson(line, Payload.class);
                handle(payload);
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
        PayloadType type = payload.getType();
        if (type == null) {
            send(ResponsePayload.error("Payload type is required"));
            return;
        }

        switch (type) {
            case LOGIN -> handleLogin(payload);
            case LIST_ITEMS -> handleListItems();
            case BID -> handleBid(payload);
            case DISCONNECT -> {
                send(ResponsePayload.ok("Disconnected"));
                close();
            }
            default -> send(ResponsePayload.error("Unsupported payload type: " + type));
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
        response.put("role", authenticatedUser.getRole());
        response.put("fullName", authenticatedUser.getFullName());
        send(response);
        LOGGER.info("User logged in: {}", username);
    }

    private void handleListItems() throws IOException {
        ResponsePayload response = ResponsePayload.ok("Items retrieved");
        response.put("items", auctionManager.getAllItems());
        send(response);
    }

    private void handleBid(Payload payload) throws IOException {
        if (!(authenticatedUser instanceof Bidder bidder)) {
            send(ResponsePayload.error("Please login with a bidder account before placing a bid"));
            return;
        }
        ResponsePayload response = auctionController.placeBid(payload, authenticatedUser);
        send(response); // gửi kết quả về cho client vừa bid

        if (response.isSuccess() && response.getBody().get("item") != null) {
            ResponsePayload update = ResponsePayload.auctionUpdate("Auction updated");
            update.put("itemId", response.getString("itemId"));
            update.put("amount", response.getDouble("amount"));
            update.put("item", response.getBody().get("item"));
            update.put("bidderId", authenticatedUser.getId());
            auctionServer.broadcast(update);
        }
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
        writer.close();
        reader.close();
        socket.close();
        LOGGER.info("Client disconnected: {}", socket.getInetAddress());
    }
}
