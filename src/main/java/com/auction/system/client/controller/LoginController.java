package com.auction.system.client.controller;

import com.auction.system.client.AuctionApplication;
import com.auction.system.client.context.AppContext;
import com.auction.system.client.network.AuctionClient;
import com.auction.system.common.payload.Payload;
import com.auction.system.common.payload.PayloadType;
import com.auction.system.common.payload.ResponsePayload;
import com.auction.system.model.user.User;
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
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Button btnLogin;

    @FXML
    void handleLogin(ActionEvent event) {
        String username = txtUsername.getText();
        String password = txtPassword.getText();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
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
            } catch (IOException | ClassNotFoundException exception) {
                Platform.runLater(() -> {
                    setLoginDisabled(false);
                    showAlert(Alert.AlertType.ERROR, "Khong the ket noi toi server.");
                });
                LOGGER.error("Login failed because the client cannot reach the server", exception);
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
        openAuctionScreen(stage);
    }

    private void setLoginDisabled(boolean disabled) {
        txtUsername.setDisable(disabled);
        txtPassword.setDisable(disabled);
        btnLogin.setDisable(disabled);
    }

    private void openAuctionScreen(Stage stage) {
        try {
            new AuctionApplication().start(stage);
        } catch (Exception exception) {
            showAlert(Alert.AlertType.ERROR, "Khong mo duoc man hinh dau gia.");
            LOGGER.error("Cannot open auction screen", exception);
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

    private ResponsePayload readResponse(AuctionClient client) throws IOException, ClassNotFoundException {
        Payload raw = client.read();
        if (raw == null) throw new IOException("Server returned null");
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
