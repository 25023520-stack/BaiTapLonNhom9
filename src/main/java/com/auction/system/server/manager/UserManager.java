package com.auction.system.server.manager;

import com.auction.system.model.user.User;

import java.util.Collection;
import java.util.Optional;

public class UserManager {
    private static final UserManager INSTANCE = new UserManager();

    private final AuthManager authManager = AuthManager.getInstance();

    private UserManager() {
    }

    public static UserManager getInstance() {
        return INSTANCE;
    }

    public Collection<User> getAllUsers() {
        return authManager.getAllUsers();
    }

    public Optional<User> findById(String id) {
        return authManager.findById(id);
    }
}
