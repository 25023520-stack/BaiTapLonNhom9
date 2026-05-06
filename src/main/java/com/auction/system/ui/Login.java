package com.auction.system.ui;

import com.auction.system.manager.AuthManager;
import com.auction.system.model.user.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

public class Login {
    private static final Logger LOGGER = LoggerFactory.getLogger(Login.class);
    private final AuthManager authManager = AppContext.getAuthManager();

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    void handleLogin(ActionEvent event) {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        Optional<User> user = authManager.login(username, password);
        if (user.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Sai tai khoan hoac mat khau.");
            return;
        }

        LOGGER.info("Login success: username={}, role={}", user.get().getUserName(), user.get().getRole());
        showAlert(Alert.AlertType.INFORMATION, "Dang nhap thanh cong voi vai tro: " + user.get().getRole());
        openAuctionScreen(event);
    }

    @FXML
    void goToRegister(ActionEvent event) {
        try {
            Parent registerView = FXMLLoader.load(getClass().getResource("/com/auction/system/ui/Register.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(registerView));
            stage.setTitle("Dang ky tai khoan");
            stage.show();
        } catch (IOException exception) {
            showAlert(Alert.AlertType.ERROR, "Khong mo duoc man hinh dang ky.");
            LOGGER.error("Cannot open register screen", exception);
        }
    }

    private void openAuctionScreen(ActionEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            new AuctionApplication().start(stage);
        } catch (Exception exception) {
            showAlert(Alert.AlertType.ERROR, "Khong mo duoc man hinh dau gia.");
            LOGGER.error("Cannot open auction screen", exception);
        }
    }

    private void showAlert(Alert.AlertType alertType, String message) {
        Alert alert = new Alert(alertType);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
