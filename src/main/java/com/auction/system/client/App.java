package com.auction.system.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.Socket;
import java.io.IOException;

public class App {
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
    private static final int SERVER_PORT = 5050; // Cổng mặc định của dự án bạn
    private static final String SERVER_HOST = "localhost";

    public static void main(String[] args) {
        LOGGER.info("Starting Auction System Bootstrapper...");

        // 1. Kiểm tra xem người dùng có ép buộc chế độ bằng VM Option không
        String forcedMode = System.getProperty(ClientRunMode.propertyName());

        if (forcedMode != null) {
            LOGGER.info("Run mode forced by VM Options: {}", forcedMode);
        } else {
            // 2. Nếu không gõ VM Options -> Tự động phát hiện (Auto-detect)
            LOGGER.info("No run mode specified. Detecting server at {}:{}...", SERVER_HOST, SERVER_PORT);

            if (isServerReachable(SERVER_HOST, SERVER_PORT)) {
                System.setProperty(ClientRunMode.propertyName(), "SEPARATE");
                LOGGER.info(">>> SUCCESS: External server detected! Setting mode to SEPARATE.");
            } else {
                System.setProperty(ClientRunMode.propertyName(), "DEMO");
                LOGGER.info(">>> NOTICE: No server found. Setting mode to DEMO (starting internal server).");
            }
        }

        LOGGER.info("Launching main application interface...");
        Launcher.main(args);
    }

    /**
     * Hàm kiểm tra xem Server có đang chạy hay không
     */
    private static boolean isServerReachable(String host, int port) {
        try (Socket socket = new Socket(host, port)) {
            return true; // Kết nối thành công
        } catch (IOException e) {
            return false; // Không kết nối được
        }
    }
}