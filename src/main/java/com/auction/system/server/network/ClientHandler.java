package com.auction.system.server.network;

import com.auction.system.common.payload.BidPayload;
import com.auction.system.common.payload.Payload;
import com.auction.system.common.payload.PayloadType;
import com.auction.system.common.payload.ResponsePayload;
import com.auction.system.manager.AuctionManager;
import com.auction.system.model.Bid;
import com.auction.system.model.Bidder;
import com.auction.system.model.User;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Optional;

public class ClientHandler implements Runnable, Closeable {
    private final Socket socket;
    private final AuctionManager auctionManager;
    private final AuctionServer auctionServer;
    private final ObjectOutputStream outputStream;
    private final ObjectInputStream inputStream;

    private volatile boolean connected = true;
    private volatile boolean closed;
    private User authenticatedUser;

    public ClientHandler(Socket socket, AuctionManager auctionManager, AuctionServer auctionServer) throws IOException {
        this.socket = socket;
        this.auctionManager = auctionManager;
        this.auctionServer = auctionServer;
        this.outputStream = new ObjectOutputStream(socket.getOutputStream());
        this.outputStream.flush();
        this.inputStream = new ObjectInputStream(socket.getInputStream());
    }

    @Override
    public void run() {
        try {
            while (connected) {
                Object incoming = inputStream.readObject();
                if (!(incoming instanceof Payload payload)) {
                    send(ResponsePayload.error("Unsupported message type"));
                    continue;
                }

                handle(payload);
            }
        } catch (EOFException ignored) {
            connected = false;
        } catch (IOException | ClassNotFoundException exception) {
            if (connected) {
                try {
                    send(ResponsePayload.error("Connection error: " + exception.getMessage()));
                } catch (IOException ignored) {
                }
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

        int itemId;
        double amount;
        if (payload instanceof BidPayload bidPayload) {
            itemId = bidPayload.getItemId();
            amount = bidPayload.getAmount();
        } else {
            Integer parsedItemId = payload.getInt("itemId");
            Double parsedAmount = payload.getDouble("amount");
            if (parsedItemId == null || parsedAmount == null) {
                send(ResponsePayload.error("Bid payload must contain itemId and amount"));
                return;
            }
            itemId = parsedItemId;
            amount = parsedAmount;
        }

        try {
            Bid bid = auctionManager.placeBid(itemId, bidder, amount);
            ResponsePayload response = ResponsePayload.ok("Bid accepted");
            response.put("bid", bid);
            response.put("itemId", itemId);
            response.put("amount", bid.getAmount());
            send(response);
        } catch (RuntimeException exception) {
            send(ResponsePayload.error(exception.getMessage()));
        }
    }

    public synchronized void send(Payload payload) throws IOException {
        outputStream.writeObject(payload);
        outputStream.flush();
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }

        closed = true;
        connected = false;
        auctionServer.removeClient(this);
        inputStream.close();
        outputStream.close();
        socket.close();
    }
}
