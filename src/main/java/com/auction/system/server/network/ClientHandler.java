package com.auction.system.server.network;

import com.auction.system.common.payload.Payload;
import com.auction.system.common.payload.PayloadType;
import com.auction.system.common.payload.ResponsePayload;
import com.auction.system.server.manager.AuctionManager;
import com.auction.system.server.controller.AuctionController;
import com.auction.system.server.controller.AuthController;
import com.auction.system.model.user.User;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable, Closeable {
    private final Socket socket;
    private final AuctionServer auctionServer;
    private final ObjectOutputStream outputStream;
    private final ObjectInputStream inputStream;
    private final AuthController authController = new AuthController();
    private final AuctionController auctionController = new AuctionController();

    private volatile boolean connected = true;
    private volatile boolean closed;
    private User authenticatedUser;

    public ClientHandler(Socket socket, AuctionServer auctionServer) throws IOException {
        this.socket = socket;
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
            case REGISTER -> handleRegister(payload);
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
        ResponsePayload response = authController.login(payload);
        if (response.isSuccess()) {
            Object user = response.getBody().get("user");
            if (user instanceof User authenticated) {
                authenticatedUser = authenticated;
            }
        }
        send(response);
    }

    private void handleRegister(Payload payload) throws IOException {
        send(authController.register(payload));
    }

    private void handleListItems() throws IOException {
        send(auctionController.listItems());
    }

    private void handleBid(Payload payload) throws IOException {
        ResponsePayload response = auctionController.placeBid(payload, authenticatedUser);
        if (response.isSuccess() && response.getBody().get("item") != null) {
            ResponsePayload update = ResponsePayload.auctionUpdate("Auction updated");
            update.put("itemId", response.getString("itemId"));
            update.put("amount", response.getDouble("amount"));
            update.put("item", response.getBody().get("item"));
            if (authenticatedUser != null) {
                update.put("bidderId", authenticatedUser.getId());
            }
            auctionServer.broadcast(update);
        }
        send(response);
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
