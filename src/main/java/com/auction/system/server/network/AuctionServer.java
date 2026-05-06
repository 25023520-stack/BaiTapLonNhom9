package com.auction.system.server.network;

import com.auction.system.manager.AuctionManager;
import com.auction.system.common.payload.Payload;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuctionServer {
    private final int port;
    private final AuctionManager auctionManager;
    private final ExecutorService clientPool;
    private final Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());

    private ServerSocket serverSocket;
    private volatile boolean running;

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

        while (running) {
            try {
                Socket socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket, auctionManager, this);
                clients.add(clientHandler);
                clientPool.submit(clientHandler);
            } catch (IOException exception) {
                if (running) {
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
        for (ClientHandler client : clients.toArray(new ClientHandler[0])) {
            try {
                client.send(payload);
            } catch (IOException ignored) {
            }
        }
    }

    void removeClient(ClientHandler clientHandler) {
        clients.remove(clientHandler);
    }
}
