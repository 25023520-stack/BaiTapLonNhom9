package com.auction.system.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AuctionApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/com/auction/system/client/view/Auction.fxml"));
        Scene scene = new Scene(root, 980, 620);
        stage.setTitle("Auction System Demo");
        stage.setScene(scene);
        stage.show();
    }
}
