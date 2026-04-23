package com.auction.system.ui;

import com.auction.system.manager.AuthManager;
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

public class Register {
    private final AuthManager authManager = AppContext.getAuthManager();

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private PasswordField txtConfirmPassword;

    @FXML
    void handleRegister(ActionEvent event) {
        String username = txtUsername.getText();
        String password = txtPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();

        try {
            authManager.register(username, username, password, confirmPassword, "BIDDER");
            showAlert(Alert.AlertType.INFORMATION, "Dang ky thanh cong. Hay dang nhap.");
            goLogin(event);
        } catch (IllegalArgumentException exception) {
            showAlert(Alert.AlertType.ERROR, exception.getMessage());
        }
    }

    @FXML
    void goLogin(ActionEvent event) {
        try {
            Parent login = FXMLLoader.load(getClass().getResource("/com/auction/system/ui/Login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(login));
            stage.setTitle("Dang nhap he thong");
            stage.show();
        } catch (IOException exception) {
            showAlert(Alert.AlertType.ERROR, "Khong mo duoc man hinh dang nhap.");
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
