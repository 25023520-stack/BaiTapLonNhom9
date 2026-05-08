package com.auction.system.ui;

import com.auction.system.manager.AuthManager;
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

public class Register {
    private static final Logger LOGGER = LoggerFactory.getLogger(Register.class);
    private final AuthManager authManager = AppContext.getAuthManager();

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

        try {
            authManager.register(fullName, username, password, email, confirmPassword, role);
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
            LOGGER.error("Cannot open login screen", exception);
        }
    }

    private void showAlert(Alert.AlertType alertType, String message) {
        Alert alert = new Alert(alertType);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
