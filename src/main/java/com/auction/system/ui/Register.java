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

public class Register {
    @FXML
    private TextField username = new TextField();
    @FXML
    private PasswordField password = new PasswordField();

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    void handleRegister(ActionEvent event) {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        System.out.println("--- ĐANG XỬ LÝ ĐĂNG KÝ ---");
        System.out.println("Tài khoản: " + username);
        System.out.println("Mật khẩu: " + password);
    }


    //move to new screen
    @FXML
    void goLogin(ActionEvent event) {
        try {
            Parent Login = FXMLLoader.load(getClass().getResource("/com/auction/system/ui/Login.fxml"));
            Stage stage =  (Stage) ((Node) event.getSource()).getScene().getWindow();

            stage.setScene(new Scene(Login));
            stage.setTitle("Đăng Nhập Hệ Thống");
            stage.show();
        }
        catch (Exception e) {
            System.out.println("Lỗi Đăng Nhập Vui Lòng Thử Lại");
        }
    }
}