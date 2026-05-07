package com.auction.system.server.controller;

import com.auction.system.common.payload.Payload;
import com.auction.system.common.payload.ResponsePayload;
import com.auction.system.model.user.User;
import com.auction.system.server.manager.AuthManager;

import java.util.Optional;

public class AuthController {
    private final AuthManager authManager = AuthManager.getInstance();

    public ResponsePayload login(Payload payload) {
        String username = payload.getString("username");
        String password = payload.getString("password");
        if (username == null || password == null) {
            return ResponsePayload.error("Username and password are required");
        }

        try {
            Optional<User> user = authManager.login(username, password);
            if (user.isEmpty()) {
                return ResponsePayload.error("Invalid username or password");
            }

            ResponsePayload response = ResponsePayload.ok("Logged in successfully");
            response.put("user", user.get());
            response.put("role", user.get().getRole());
            response.put("fullName", user.get().getFullName());
            return response;
        } catch (IllegalArgumentException exception) {
            return ResponsePayload.error(exception.getMessage());
        }
    }

    public ResponsePayload register(Payload payload) {
        try {
            User user = authManager.register(
                    payload.getString("fullName"),
                    payload.getString("username"),
                    payload.getString("password"),
                    payload.getString("confirmPassword"),
                    payload.getString("role")
            );
            ResponsePayload response = ResponsePayload.ok("Dang ky thanh cong. Hay dang nhap.");
            response.put("user", user);
            return response;
        } catch (IllegalArgumentException exception) {
            return ResponsePayload.error(exception.getMessage());
        }
    }
}
