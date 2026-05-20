package com.auction.system.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class ServerDownController {

    @FXML
    private Button retryButton;

    @FXML
    void handleRetry(ActionEvent event) {
        try {
            Parent loginView = FXMLLoader.load(
                    getClass().getResource("/com/auction/system/client/view/Login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(loginView));
            stage.setTitle("Đăng nhập");
            stage.show();
        } catch (Exception ignored) {
        }
    }
}
