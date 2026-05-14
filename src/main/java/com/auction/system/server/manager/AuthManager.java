package com.auction.system.server.manager;

import com.auction.system.model.user.Admin;
import com.auction.system.model.user.Bidder;
import com.auction.system.model.user.Seller;
import com.auction.system.model.user.User;

import com.auction.system.server.dao.UserDAO;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public class AuthManager {
    private static final AuthManager INSTANCE = new AuthManager();


    private final UserDAO userDAO = new UserDAO();

    private AuthManager() {}


    public static AuthManager getInstance() {
        return INSTANCE;
    }

    public synchronized User register(
            String fullName,
            String username,
            String email,
            String password,
            String confirmPassword,
            String role
    ) {
        validateRegisterInput(fullName, username, email, password, confirmPassword, role);

        String userId = generateUserId(role);
        User user = createUserByRole(
                userId,
                fullName.trim(),
                username.trim(),
                email.trim(),
                password,
                role
        );

        String insertedId = userDAO.insertUser(user);

        if (insertedId == null) {
            throw new IllegalArgumentException("khong the luu user vao database");
        }

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
        if (isBlank(user.getEmail())) {
            throw new IllegalArgumentException("Email must not be blank");
        }
        if(userDAO.findById(user.getId()) != null) {
            return;
        }
        if(userDAO.existsByUsername(user.getUserName())) {
            return;
        }
        if(userDAO.existsByEmail(user.getEmail())) {
            return;
        }
        String insertedId = userDAO.insertUser(user);
        if(insertedId == null) {
            throw new IllegalArgumentException("Cannot save user to database");
        }

    }

    public synchronized Optional<User> login(String username, String password) {
        validateLoginInput(username, password);

        User user = userDAO.findByUsername(username.trim());

        if (user == null || !user.checkPassword(password)) {
            return Optional.empty();
        }

        return Optional.of(user);
    }

    public synchronized boolean existsByUsername(String username) {
        return !isBlank(username) && userDAO.existsByUsername(username.trim());
    }

    public synchronized boolean existsByEmail(String email) {
        return !isBlank(email) && userDAO.existsByEmail(email.trim());
    }

    public synchronized Optional<User> findById(String id) {
        if(isBlank(id)) {
            return Optional.empty();
        }

        return Optional.ofNullable(userDAO.findById(id));
    }

    public synchronized Collection<User> getAllUsers() {
        return userDAO.findAll();
    }

    synchronized void resetForTest() {
    }

    private void validateRegisterInput(
            String fullName,
            String username,
            String email,
            String password,
            String confirmPassword,
            String role
    ) {
        if (isBlank(fullName)) {
            throw new IllegalArgumentException("Full name must not be blank");
        }
        if (isBlank(username)) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        if (isBlank(email)) {
            throw new IllegalArgumentException("Email must not be blank");
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
        if (existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (isBlank(role)) {
            throw new IllegalArgumentException("Role must not be blank");
        }
    }

    private void validateLoginInput(String username, String password) {
        if (isBlank(username) || isBlank(password)) {
            throw new IllegalArgumentException("Username or password must not be blank");
        }
    }

    private User createUserByRole(String id, String fullName, String username, String email, String password, String role) {
        return switch (role.trim().toUpperCase()) {
            case "ADMIN" -> new Admin(id, fullName, username, email, password);
            case "SELLER" -> new Seller(id, fullName, username, email, password);
            case "BIDDER" -> new Bidder(id, fullName, username, email, password);
            default -> throw new IllegalArgumentException("Role must be ADMIN, SELLER, or BIDDER");
        };
    }

    private String generateUserId(String role) {
        return role.trim().toUpperCase() + "-" + UUID.randomUUID();
    }

    private void seedDefaultUsers() {
        registerUser(new Admin("ADMIN-1", "Admin", "admin", "admin@example.com", "123"));
        registerUser(new Seller("SELLER-1", "Seller Demo", "seller", "seller@example.com", "123"));
        registerUser(new Bidder("BIDDER-1", "Bidder Demo", "bidder", "bidder@example.com", "123"));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
