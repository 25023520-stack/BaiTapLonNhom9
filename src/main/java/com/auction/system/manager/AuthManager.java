package com.auction.system.manager;

import com.auction.system.model.user.Admin;
import com.auction.system.model.user.Bidder;
import com.auction.system.model.user.Seller;
import com.auction.system.model.user.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class AuthManager {
    private static final AuthManager INSTANCE = new AuthManager(false);

    private final Map<Integer, User> usersById = new HashMap<>();
    private final Map<String, User> usersByUsername = new HashMap<>();
    private final Map<String, User> usersByEmail = new HashMap<>();
    private final AtomicInteger nextUserId = new AtomicInteger(1);

    private AuthManager() {
        this(false);
    }

    AuthManager(boolean seedDefaultUsers) {
        if (seedDefaultUsers) {
            seedDefaultUsers();
        }
    }

    public static AuthManager getInstance() {
        return INSTANCE;
    }

    // Dang ky tai khoan moi tu du lieu form, tu sinh userId va tao dung role.
    public synchronized User register(String fullName, String username, String password, String email, String confirmPassword, String role) {
        validateRegisterInput(fullName, username, password, email, confirmPassword, role);

        int userId = nextUserId.getAndIncrement();
        User user = createUserByRole(userId, fullName.trim(), username.trim(), email.trim(), password, role);
        usersById.put(user.getId(), user);
        usersByUsername.put(user.getUserName(), user);
        usersByEmail.put(user.getEmail(), user);
        return user;
    }

    // Them truc tiep mot User da tao san vao he thong, thuong dung cho seed/test.
    public synchronized void registerUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        if (user.getId() <= 0) {
            throw new IllegalArgumentException("User id must be greater than 0");
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
        if (isBlank(user.getEmail())) {
            throw new IllegalArgumentException("Email must not be blank");
        }
        if (usersByEmail.containsKey(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        usersById.put(user.getId(), user);
        nextUserId.updateAndGet(next -> Math.max(next, user.getId() + 1));
        usersByUsername.put(user.getUserName(), user);
        usersByEmail.put(user.getEmail(), user);
    }

    // Kiem tra dang nhap bang username + password, tra ve User neu hop le.
    public synchronized Optional<User> login(String username, String password) {

        validateloginInput(username, password);

        if (isBlank(username) || isBlank(password)) {
            return Optional.empty();
        }

        User user = usersByUsername.get(username.trim());
        if (user == null || !user.checkPassword(password)) {
            return Optional.empty();
        }

        return Optional.of(user);
    }
    //report input error to user
    public  synchronized void LoginUser(User user) {
        if (isBlank(user.getUserName())) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        if (isBlank(user.getPassWord())) {
            throw new IllegalArgumentException("Password must not be blank");
        }
    }

    // Kiem tra nhanh username da ton tai trong he thong hay chua.
    public synchronized boolean existsByUsername(String username) {
        return !isBlank(username) && usersByUsername.containsKey(username.trim());
    }

    public synchronized boolean existsByEmail(String email) {
        return !isBlank(email) && usersByEmail.containsKey(email.trim());
    }

    // Tim user theo id, dung Optional de tranh tra ve null truc tiep.
    public synchronized Optional<User> findById(int id) {
        return Optional.ofNullable(usersById.get(id));
    }

    // Tra ve toan bo user hien dang duoc luu trong bo nho.
    public synchronized Collection<User> getAllUsers() {
        return usersById.values();
    }

    // Gom cac rule validate cho form dang ky truoc khi tao tai khoan moi.
    private void validateRegisterInput(String fullName, String username, String password, String email, String confirmPassword, String role) {
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
    private void validateloginInput(String username, String password) {
        if(isBlank(username) || isBlank(password)) {
            throw new IllegalArgumentException("Username or password must not be blank");
        }
        if (isBlank(username)) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        if (isBlank(password)) {
            throw new IllegalArgumentException("Password must not be blank");
        }

    }

    // Factory nho: tao dung object User theo role duoc chon tren giao dien.
    private User createUserByRole(int id, String fullName, String username, String email, String password, String role) {
        return switch (role.trim().toUpperCase()) {
            case "ADMIN" -> new Admin(id, fullName, username, email, password);
            case "SELLER" -> new Seller(id, fullName, username, email, password);
            case "BIDDER" -> new Bidder(id, fullName, username, email, password);
            default -> throw new IllegalArgumentException("Role must be ADMIN, SELLER, or BIDDER");
        };
    }

    // Tao san mot so tai khoan mau de login nhanh khi demo giao dien.
    private void seedDefaultUsers() {
        registerUser(new Admin(1, "Admin", "admin", "admin@example.com", "123"));
        registerUser(new Seller(2, "Seller Demo", "seller", "seller@example.com", "123"));
        registerUser(new Bidder(3, "Bidder Demo", "bidder", "bidder@example.com", "123"));
    }

    // Ham ho tro kiem tra chuoi rong/null de dung lai o nhieu noi.
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
