package com.auction.system.client.network;

import com.auction.system.common.payload.Payload;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;

public class AuctionClient implements Closeable {
    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private Thread listenerThread;
    private volatile boolean connected;

    public void connect(String host, int port) throws IOException {
        if (connected) {
            return;
        }

        socket = new Socket(host, port);
        outputStream = new ObjectOutputStream(socket.getOutputStream());
        outputStream.flush();
        inputStream = new ObjectInputStream(socket.getInputStream());
        connected = true;
    }

    public synchronized void send(Payload payload) throws IOException {
        if (!connected) {
            throw new IllegalStateException("Client is not connected");
        }

        outputStream.writeObject(payload);
        outputStream.flush();
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
                while (connected) {
                    Object incoming = inputStream.readObject();
                    if (incoming instanceof Payload payload) {
                        onMessage.accept(payload);
                    }
                }
            } catch (EOFException ignored) {
                connected = false;
            } catch (IOException | ClassNotFoundException exception) {
                connected = false;
                if (onError != null) {
                    onError.accept(exception instanceof ClassNotFoundException
                            ? new IOException("Unsupported payload received", exception)
                            : exception);
                }
            }
        }, "auction-client-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public Payload read() throws IOException, ClassNotFoundException {
        if (!connected) {
            throw new IllegalStateException("Client is not connected");
        }

        Object incoming = inputStream.readObject();
        if (incoming instanceof Payload payload) {
            return payload;
        }
        throw new IOException("Unsupported message type");
    }

    public boolean isConnected() {
        return connected;
    }

    @Override
    public void close() throws IOException {
        connected = false;

        if (inputStream != null) {
            inputStream.close();
        }
        if (outputStream != null) {
            outputStream.close();
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
