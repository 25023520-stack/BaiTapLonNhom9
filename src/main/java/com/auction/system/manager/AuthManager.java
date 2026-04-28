package com.auction.system.manager;

import com.auction.system.model.user.Admin;
import com.auction.system.model.user.Bidder;
import com.auction.system.model.user.Seller;
import com.auction.system.model.user.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class AuthManager {
    private final Map<String, User> usersById = new HashMap<>();
    private final Map<String, User> usersByUsername = new HashMap<>();

    public AuthManager() {
        seedDefaultUsers();
    }

    public synchronized User register(String fullName, String username, String password, String confirmPassword, String role) {
        validateRegisterInput(fullName, username, password, confirmPassword, role);

        String userId = UUID.randomUUID().toString();
        User user = createUserByRole(userId, fullName.trim(), username.trim(), password, role);
        usersById.put(user.getId(), user);
        usersByUsername.put(user.getUserName(), user);
        return user;
    }

    public synchronized void registerUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        if (isBlank(user.getId())) {
            throw new IllegalArgumentException("User id must not be blank");
        }
        if (isBlank(user.getUserName())) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        if (usersById.containsKey(user.getId())) {
            throw new IllegalArgumentException("User id already exists");
        }
        if (usersByUsername.containsKey(user.getUserName())) {
            throw new IllegalArgumentException("Username already exists");
        }

        usersById.put(user.getId(), user);
        usersByUsername.put(user.getUserName(), user);
    }

    public synchronized Optional<User> login(String username, String password) {
        if (isBlank(username) || isBlank(password)) {
            return Optional.empty();
        }

        User user = usersByUsername.get(username.trim());
        if (user == null || !user.checkPassword(password)) {
            return Optional.empty();
        }

        return Optional.of(user);
    }

    public synchronized boolean existsByUsername(String username) {
        return !isBlank(username) && usersByUsername.containsKey(username.trim());
    }

    public synchronized Optional<User> findById(String id) {
        return Optional.ofNullable(usersById.get(id));
    }

    public synchronized Collection<User> getAllUsers() {
        return usersById.values();
    }

    private void validateRegisterInput(String fullName, String username, String password, String confirmPassword, String role) {
        if (isBlank(fullName)) {
            throw new IllegalArgumentException("Full name must not be blank");
        }
        if (isBlank(username)) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        if (isBlank(password)) {
            throw new IllegalArgumentException("Password must not be blank");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Confirm password does not match");
        }
        if (existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (isBlank(role)) {
            throw new IllegalArgumentException("Role must not be blank");
        }
    }

    private User createUserByRole(String id, String fullName, String username, String password, String role) {
        return switch (role.trim().toUpperCase()) {
            case "ADMIN" -> new Admin(id, fullName, username, password);
            case "SELLER" -> new Seller(id, fullName, username, password);
            case "BIDDER" -> new Bidder(id, fullName, username, password);
            default -> throw new IllegalArgumentException("Role must be ADMIN, SELLER, or BIDDER");
        };
    }

    private void seedDefaultUsers() {
        registerUser(new Admin("A1", "Admin", "admin", "123"));
        registerUser(new Seller("S1", "Seller Demo", "seller", "123"));
        registerUser(new Bidder("B1", "Bidder Demo", "bidder", "123"));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
