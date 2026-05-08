package com.auction.system.client;

import com.auction.system.server.ServerMain;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

public class Launcher extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Launcher.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        ClientRunMode runMode = ClientRunMode.current();
        if (runMode.shouldStartEmbeddedServer()) {
            ServerMain.startInBackground();
            LOGGER.info("Starting client in DEMO mode with local server bootstrap");
        } else {
            LOGGER.info("Starting client in SEPARATE mode; expecting an external server");
        }

        URL resource = getClass().getResource("/com/auction/system/client/view/Login.fxml");
        if (resource == null) {
            LOGGER.error("Resource not found: Login.fxml");
            System.exit(1);
        }

        Parent root = FXMLLoader.load(resource);
        Scene scene = new Scene(root);
        primaryStage.setTitle("Auction System");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
