package com.auction.system.client.controller;

import com.auction.system.client.context.AppContext;
import com.auction.system.client.network.AuctionClient;
import com.auction.system.common.payload.Payload;
import com.auction.system.common.payload.PayloadType;
import com.auction.system.common.payload.ResponsePayload;

import javafx.application.Platform;
import javafx.event.ActionEvent;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RegisterController {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterController.class);

    @FXML
    private TextField txtFullName;

    @FXML
    private TextField txtUsername;

    @FXML
    private TextField txtEmail;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private PasswordField txtConfirmPassword;

    @FXML
    private ComboBox<String> cbRole;

    @FXML
    private Button btnRegister;

    @FXML
    void initialize() {
        cbRole.getItems().setAll("BIDDER", "SELLER");
        cbRole.setValue("BIDDER");
    }

    @FXML
    void handleRegister(ActionEvent event) {
        String fullName = txtFullName.getText();
        String username = txtUsername.getText();
        String email = txtEmail.getText();
        String password = txtPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();
        String role = cbRole.getValue();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        setRegisterDisabled(true);

        Thread registerThread = new Thread(() -> {
            try {
                AuctionClient client = AppContext.getAuctionClient();
                Payload payload = new Payload(PayloadType.REGISTER);
                payload.put("fullName", fullName);
                payload.put("username", username);
                payload.put("email", email);
                payload.put("password", password);
                payload.put("confirmPassword", confirmPassword);
                payload.put("role", role);

                client.send(payload);
                ResponsePayload response = readResponse(client);

                Platform.runLater(() -> handleRegisterResponse(stage, response));
            } catch (IOException | ClassNotFoundException exception) {
                Platform.runLater(() -> {
                    setRegisterDisabled(false);
                    showAlert(Alert.AlertType.ERROR, "Khong the ket noi toi server.");
                });
                LOGGER.error("Register failed because the client cannot reach the server", exception);
            } catch (IllegalArgumentException exception) {
                Platform.runLater(() -> {
                    setRegisterDisabled(false);
                    showAlert(Alert.AlertType.ERROR, exception.getMessage());
                });
            }
        }, "register-request");
        registerThread.setDaemon(true);
        registerThread.start();
    }

    private void handleRegisterResponse(Stage stage, ResponsePayload response) {
        setRegisterDisabled(false);
        if (!response.isSuccess()) {
            showAlert(Alert.AlertType.ERROR, response.getMessage());
            return;
        }

        showAlert(Alert.AlertType.INFORMATION, response.getMessage());
        openLoginScreen(stage);
    }

    private void setRegisterDisabled(boolean disabled) {
        txtFullName.setDisable(disabled);
        txtUsername.setDisable(disabled);
        txtEmail.setDisable(disabled);
        txtPassword.setDisable(disabled);
        txtConfirmPassword.setDisable(disabled);
        cbRole.setDisable(disabled);
        btnRegister.setDisable(disabled);
    }

    @FXML
    void goLogin(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        openLoginScreen(stage);
    }

    private void openLoginScreen(Stage stage) {
        try {
            Parent login = FXMLLoader.load(getClass().getResource("/com/auction/system/client/view/Login.fxml"));
            stage.setScene(new Scene(login));
            stage.setTitle("Dang nhap he thong");
            stage.show();
        } catch (IOException exception) {
            showAlert(Alert.AlertType.ERROR, "Khong mo duoc man hinh dang nhap.");
            LOGGER.error("Cannot open login screen", exception);
        }
    }

    private ResponsePayload readResponse(AuctionClient client) throws IOException, ClassNotFoundException {
        if (client.read() instanceof ResponsePayload response) {
            return response;
        }
        throw new IOException("Unexpected payload received from server");
    }

    private void showAlert(Alert.AlertType alertType, String message) {
        Alert alert = new Alert(alertType);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
