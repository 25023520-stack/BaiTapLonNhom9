package com.auction.system.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Launcher extends Application {
    private static  final Logger logger = LoggerFactory.getLogger(Launcher.class);
    @Override
    public void start(Stage primaryStage) throws Exception {

        URL resource = getClass().getResource("/com/auction/system/ui/Login.fxml");

        if (resource == null) {
            logger.info("Resource not found");
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
