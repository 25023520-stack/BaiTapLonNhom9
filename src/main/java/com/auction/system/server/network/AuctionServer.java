package com.auction.system.server.network;

import com.auction.system.common.payload.Payload;
import com.auction.system.server.manager.AuctionManager;
import com.auction.system.server.observer.AuctionObserver;
import com.auction.system.server.observer.AuctionSubject;
import com.auction.system.model.item.Item;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuctionServer implements AuctionSubject{
    private final int port;
    private final AuctionManager auctionManager;
    private final ExecutorService clientPool;
    private final Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());
    private final List<AuctionObserver> observers = new CopyOnWriteArrayList<>();

    private ServerSocket serverSocket;
    private volatile boolean running;

    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionServer.class);


    public AuctionServer(int port, AuctionManager auctionManager) {
        this(port, auctionManager, Executors.newCachedThreadPool());
    }

    public AuctionServer(int port, AuctionManager auctionManager, ExecutorService clientPool) {
        this.port = port;
        this.auctionManager = auctionManager;
        this.clientPool = clientPool;
    }

    public void start() throws IOException {
        if (running) {
            return;
        }

        serverSocket = new ServerSocket(port);
        running = true;

        LOGGER.info("=============================================");
        LOGGER.info("AUCTION SERVER IS ONLINE ON PORT: {}", port);
        LOGGER.info("Waiting for clients to connect...");
        LOGGER.info("=============================================");


        while (running) {
            try {
                Socket socket = serverSocket.accept();
                LOGGER.info("New client connected from: {}", socket.getRemoteSocketAddress());

                ClientHandler clientHandler = new ClientHandler(socket,auctionManager, this);
                clients.add(clientHandler);
                clientPool.submit(clientHandler);
            } catch (IOException exception) {
                if (running) {
                    LOGGER.error("Error accepting client connection", exception);
                    throw exception;
                }
            }
        }
    }

    public void stop() throws IOException {
        running = false;

        for (ClientHandler client : clients.toArray(new ClientHandler[0])) {
            client.close();
        }
        clients.clear();

        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        clientPool.shutdownNow();
    }

    public void broadcast(Payload payload) {
        ClientHandler[] snapshot = clients.toArray(new ClientHandler[0]);
        for (ClientHandler client : snapshot) {
            try {
                client.send(payload);
            } catch (Exception e) {
                LOGGER.warn("Failed to send to client, removing: {}", e.getMessage());
                removeClient(client);
            }
        }
    }

    void removeClient(ClientHandler clientHandler) {
        clients.remove(clientHandler);
    }

    @Override
    public void addObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(Item item, String eventType) {
        for (AuctionObserver observer : observers) {
            observer.onAuctionUpdated(item, eventType);
        }
    }
}
