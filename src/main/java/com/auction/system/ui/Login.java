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

import java.io.IOException;
import java.util.Optional;

public class Login {
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

        showAlert(Alert.AlertType.INFORMATION, "Dang nhap thanh cong: " + user.get().getRole());
        openAuctionScreen(event);
    }

    @FXML
    void goToRegister(ActionEvent event) {
        switchScene(event, "/com/auction/system/ui/Register.fxml", "Dang ky tai khoan");
    }

    private void openAuctionScreen(ActionEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            new AuctionApplication().start(stage);
        } catch (Exception exception) {
            showAlert(Alert.AlertType.ERROR, "Khong mo duoc man hinh dau gia.");
            exception.printStackTrace();
        }
    }

    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(view));
            stage.setTitle(title);
            stage.show();
        } catch (IOException exception) {
            showAlert(Alert.AlertType.ERROR, "Khong mo duoc man hinh: " + fxmlPath);
            exception.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType alertType, String message) {
        Alert alert = new Alert(alertType);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
