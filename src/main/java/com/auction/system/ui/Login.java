
package com.auction.system.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.IOException;

public class Login {


    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    void handleLogin(ActionEvent event) {
        String user = txtUsername.getText();
        String pass = txtPassword.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            System.out.println("Username or password is empty");
        }

    }

    @FXML
    void goToRegister(ActionEvent event) {
        try {
            Parent registerView = FXMLLoader.load(getClass().getResource("Register.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(registerView));
            stage.setTitle("Đăng ký tài khoản");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}