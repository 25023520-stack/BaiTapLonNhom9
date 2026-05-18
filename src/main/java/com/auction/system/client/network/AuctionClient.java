package com.auction.system.client.network;

import com.auction.system.common.json.GsonProvider;
import com.auction.system.common.payload.Payload;
import com.auction.system.common.payload.PayloadType;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class AuctionClient implements Closeable {
    private static final Gson GSON = GsonProvider.get();

    private final BlockingQueue<Payload> responseQueue = new LinkedBlockingQueue<>();

    private Socket socket;
    private Thread readerThread;
    private volatile boolean connected;
    private PrintWriter writer;
    private BufferedReader reader;
    private volatile Consumer<Payload> messageConsumer;
    private volatile Consumer<Exception> errorConsumer;
    private volatile IOException terminalReadException;

    public synchronized void connect(String host, int port) throws IOException {
        if (connected) {
            return;
        }

        responseQueue.clear();
        terminalReadException = null;
        messageConsumer = null;
        errorConsumer = null;
        socket = new Socket(host, port);
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        connected = true;
        startReaderThread(socket, reader);
    }

    public synchronized void send(Payload payload) throws IOException {
        if (!connected) {
            throw new IllegalStateException("Client is not connected");
        }

        writer.println(GSON.toJson(payload));
    }

    public synchronized void startListening(Consumer<Payload> onMessage, Consumer<Exception> onError) {
        if (!connected) {
            throw new IllegalStateException("Client is not connected");
        }

        messageConsumer = onMessage;
        errorConsumer = onError;
    }

    public Payload read() throws IOException {
        if (!connected && responseQueue.isEmpty()) {
            throw new IllegalStateException("Client is not connected");
        }

        try {
            Payload payload = responseQueue.take();
            if (payload.getType() == null && terminalReadException != null) {
                throw terminalReadException;
            }
            return payload;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for server response", exception);
        }
    }

    public boolean isConnected() {
        return connected;
    }

    @Override
    public synchronized void close() throws IOException {
        connected = false;
        messageConsumer = null;
        errorConsumer = null;
        terminalReadException = null;
        responseQueue.clear();

        IOException closeException = null;
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException exception) {
            closeException = exception;
        }
        if (writer != null) {
            writer.close();
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException exception) {
            if (closeException == null) {
                closeException = exception;
            } else {
                closeException.addSuppressed(exception);
            }
        } finally {
            socket = null;
            writer = null;
            reader = null;
            readerThread = null;
        }

        if (closeException != null) {
            throw closeException;
        }
    }

    private void startReaderThread(Socket activeSocket, BufferedReader activeReader) {
        readerThread = new Thread(() -> {
            try {
                String line;
                while (isActiveConnection(activeSocket) && (line = activeReader.readLine()) != null) {
                    Payload payload = GSON.fromJson(line, Payload.class);
                    if (payload == null) {
                        continue;
                    }

                    if (payload.getType() == PayloadType.UPDATE_AUCTION) {
                        Consumer<Payload> consumer = messageConsumer;
                        if (consumer != null) {
                            consumer.accept(payload);
                        }
                        continue;
                    }

                    responseQueue.offer(payload);
                }

                if (isActiveConnection(activeSocket)) {
                    terminalReadException = new EOFException("Server closed connection");
                    responseQueue.offer(new Payload());
                }
            } catch (IOException exception) {
                if (isActiveConnection(activeSocket)) {
                    terminalReadException = exception;
                    responseQueue.offer(new Payload());
                    Consumer<Exception> consumer = errorConsumer;
                    if (consumer != null) {
                        consumer.accept(exception);
                    }
                }
            } finally {
                if (socket == activeSocket) {
                    connected = false;
                }
            }
        }, "auction-client-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private boolean isActiveConnection(Socket activeSocket) {
        return connected && socket == activeSocket;
    }
}
