package com.auction.system.client.controller;

import com.auction.system.client.context.AppContext;
import com.auction.system.client.network.AuctionClient;
import com.auction.system.common.payload.Payload;
import com.auction.system.common.payload.PayloadType;
import com.auction.system.common.payload.ResponsePayload;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
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
    private PasswordField txtPassword;

    @FXML
    private PasswordField txtConfirmPassword;

    @FXML
    private ComboBox<String> cbRole;

    @FXML
    void initialize() {
        cbRole.getItems().setAll("BIDDER", "SELLER");
        cbRole.setValue("BIDDER");
    }

    @FXML
    void handleRegister(ActionEvent event) {
        try {
            AuctionClient client = AppContext.getAuctionClient();
            Payload payload = new Payload(PayloadType.REGISTER);
            payload.put("fullName", txtFullName.getText());
            payload.put("username", txtUsername.getText());
            payload.put("password", txtPassword.getText());
            payload.put("confirmPassword", txtConfirmPassword.getText());
            payload.put("role", cbRole.getValue());

            client.send(payload);
            ResponsePayload response = readResponse(client);
            if (!response.isSuccess()) {
                showAlert(Alert.AlertType.ERROR, response.getMessage());
                return;
            }

            showAlert(Alert.AlertType.INFORMATION, response.getMessage());
            goLogin(event);
        } catch (IOException | ClassNotFoundException exception) {
            showAlert(Alert.AlertType.ERROR, "Khong the ket noi toi server.");
            LOGGER.error("Register failed because the client cannot reach the server", exception);
        }
    }

    @FXML
    void goLogin(ActionEvent event) {
        try {
            Parent login = FXMLLoader.load(getClass().getResource("/com/auction/system/client/view/Login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
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
