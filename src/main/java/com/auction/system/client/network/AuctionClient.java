package com.auction.system.client.network;

import com.auction.system.common.payload.Payload;
import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class AuctionClient implements Closeable {
    private static final Gson GSON = new Gson();

    private Socket socket;
    private Thread listenerThread;
    private volatile boolean connected;
    private PrintWriter writer;
    private BufferedReader reader;


    public void connect(String host, int port) throws IOException {
        if (connected) {
            return;
        }

        socket = new Socket(host, port);
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        connected = true;
    }

    public synchronized void send(Payload payload) throws IOException {
        if (!connected) {
            throw new IllegalStateException("Client is not connected");
        }

        writer.println(GSON.toJson(payload));
    }

    public void startListening(Consumer<Payload> onMessage, Consumer<Exception> onError) {
        if (!connected) {
            throw new IllegalStateException("Client is not connected");
        }
        if (listenerThread != null && listenerThread.isAlive()) {
            return;
        }

        listenerThread = new Thread(() -> {
            try {
                String line;
                while (connected && (line = reader.readLine()) != null) {
                    Payload payload = GSON.fromJson(line, Payload.class);
                    onMessage.accept(payload);
                }
            } catch (IOException e) {
                connected = false;
                if (onError != null) onError.accept(e);
            }
        }, "auction-client-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public Payload read() throws IOException, ClassNotFoundException {
        if (!connected) {
            throw new IllegalStateException("Client is not connected");
        }

        String line = reader.readLine();        // block cho đến khi nhận 1 dòng
        if (line == null) throw new EOFException("Server closed connection");
        return GSON.fromJson(line, Payload.class);
    }

    public boolean isConnected() {
        return connected;
    }

    @Override
    public void close() throws IOException {
        connected = false;
        if (reader != null) reader.close();
        if (writer != null) writer.close();
        if (socket != null && !socket.isClosed()) socket.close();
    }
}
