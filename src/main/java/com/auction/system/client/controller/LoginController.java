package com.auction.system.client.controller;

import com.auction.system.client.context.AppContext;
import com.auction.system.client.network.AuctionClient;
import com.auction.system.common.payload.Payload;
import com.auction.system.common.payload.PayloadType;
import com.auction.system.common.payload.ResponsePayload;
import com.auction.system.model.user.Admin;
import com.auction.system.model.user.Seller;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class LoginController {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class);

    @FXML
    private TextField txtServerHost;

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Button btnLogin;

    @FXML
    void initialize() {
        txtServerHost.setText(AppContext.getServerHost());
    }

    @FXML
    void handleLogin(ActionEvent event) {
        String host = txtServerHost.getText().trim();
        String username = txtUsername.getText();
        String password = txtPassword.getText();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        AppContext.setServerHost(host.isEmpty() ? "127.0.0.1" : host);
        setLoginDisabled(true);

        Thread loginThread = new Thread(() -> {
            try {
                AuctionClient client = AppContext.getAuctionClient();
                Payload payload = new Payload(PayloadType.LOGIN);
                payload.put("username", username);
                payload.put("password", password);

                client.send(payload);
                ResponsePayload response = readResponse(client);

                Platform.runLater(() -> handleLoginResponse(stage, response));
            } catch (IOException exception) {
                LOGGER.error("Login failed because the client cannot reach the server", exception);
                Platform.runLater(() -> {
                    setLoginDisabled(false);
                    AppContext.goToServerDown(stage);
                });
            } catch (IllegalArgumentException exception) {
                Platform.runLater(() -> {
                    setLoginDisabled(false);
                    showAlert(Alert.AlertType.ERROR, exception.getMessage());
                });
            }
        }, "login-request");
        loginThread.setDaemon(true);
        loginThread.start();
    }

    private void handleLoginResponse(Stage stage, ResponsePayload response) {
        setLoginDisabled(false);
        if (!response.isSuccess()) {
            showAlert(Alert.AlertType.ERROR, response.getMessage());
            return;
        }

        AppContext.setCurrentUserFromMap(response.getBody());

        showAlert(Alert.AlertType.INFORMATION, response.getMessage());
        if (AppContext.getCurrentUser() instanceof Admin) {
            openAdminScreen(stage);
        } else if (AppContext.getCurrentUser() instanceof Seller) {
            openSellerScreen(stage);
        } else {
            openBidderScreen(stage);
        }
    }

    private void setLoginDisabled(boolean disabled) {
        txtServerHost.setDisable(disabled);
        txtUsername.setDisable(disabled);
        txtPassword.setDisable(disabled);
        btnLogin.setDisable(disabled);
    }

    private void openBidderScreen(Stage stage) {
        try {
            Parent bidderView = FXMLLoader.load(getClass().getResource("/com/auction/system/client/view/Bidder.fxml"));
            stage.setScene(new Scene(bidderView, 1120, 700));
            stage.setTitle("Bidder Workspace");
            stage.show();
        } catch (Exception exception) {
            showAlert(Alert.AlertType.ERROR, "Khong mo duoc man hinh bidder.");
            LOGGER.error("Cannot open bidder screen", exception);
        }
    }

    private void openSellerScreen(Stage stage) {
        try {
            Parent sellerView = FXMLLoader.load(getClass().getResource("/com/auction/system/client/view/Seller.fxml"));
            stage.setScene(new Scene(sellerView, 1120, 700));
            stage.setTitle("Seller Workspace");
            stage.show();
        } catch (Exception exception) {
            showAlert(Alert.AlertType.ERROR, "Khong mo duoc man hinh seller.");
            LOGGER.error("Cannot open seller screen", exception);
        }
    }

    private void openAdminScreen(Stage stage) {
        try {
            Parent adminView = FXMLLoader.load(getClass().getResource("/com/auction/system/client/view/Admin.fxml"));
            stage.setScene(new Scene(adminView, 1180, 720));
            stage.setTitle("Admin Workspace");
            stage.show();
        } catch (Exception exception) {
            showAlert(Alert.AlertType.ERROR, "Khong mo duoc man hinh admin.");
            LOGGER.error("Cannot open admin screen", exception);
        }
    }

    @FXML
    void goToRegister(ActionEvent event) {
        try {
            Parent registerView = FXMLLoader.load(getClass().getResource("/com/auction/system/client/view/Register.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(registerView));
            stage.setTitle("Dang ky tai khoan");
            stage.show();
        } catch (IOException exception) {
            showAlert(Alert.AlertType.ERROR, "Khong mo duoc man hinh dang ky.");
            LOGGER.error("Cannot open register screen", exception);
        }
    }

    private ResponsePayload readResponse(AuctionClient client) throws IOException {
        Payload raw = client.read();
        if (raw == null) {
            throw new IOException("Server returned null");
        }
        ResponsePayload response = new ResponsePayload();
        response.setType(raw.getType());
        raw.getBody().forEach(response::put);
        return response;
    }

    private void showAlert(Alert.AlertType alertType, String message) {
        Alert alert = new Alert(alertType);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
