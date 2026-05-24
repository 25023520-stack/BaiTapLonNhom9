package com.auction.system.server.manager;

import com.auction.system.model.user.Bidder;
import com.auction.system.model.user.Seller;
import com.auction.system.model.user.User;
import com.auction.system.server.database.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuthManagerTest {

    private static final String EMAIL_DOMAIN = "@authtest.local";

    private AuthManager authManager;

    @BeforeEach
    void setUp() {
        Database.getInstance().initializeDatabase();
        cleanUpTestData();
        authManager = AuthManager.getInstance();
    }

    @AfterEach
    void tearDown() {
        cleanUpTestData();
    }

    @Test
    void registerCreatesBidderWithApprovedTrue() {
        String token = shortToken();
        User user = authManager.register(
                "Test Bidder",
                "tbidder_" + token,
                "tbidder_" + token + EMAIL_DOMAIN,
                "pass123", "pass123", "BIDDER"
        );
        assertNotNull(user);
        assertInstanceOf(Bidder.class, user);
        assertTrue(user.isApproved());
        assertEquals("BIDDER", user.getRole());
    }

    @Test
    void registerCreatesSellerWithApprovedFalse() {
        String token = shortToken();
        User user = authManager.register(
                "Test Seller",
                "tseller_" + token,
                "tseller_" + token + EMAIL_DOMAIN,
                "pass123", "pass123", "SELLER"
        );
        assertNotNull(user);
        assertInstanceOf(Seller.class, user);
        assertFalse(user.isApproved());
        assertEquals("SELLER", user.getRole());
    }

    @Test
    void registerThrowsWhenPasswordsMismatch() {
        String token = shortToken();
        assertThrows(IllegalArgumentException.class, () ->
                authManager.register("Name", "mismatch_" + token,
                        "mismatch_" + token + EMAIL_DOMAIN, "pass1", "pass2", "BIDDER")
        );
    }

    @Test
    void registerThrowsWhenFullNameIsBlank() {
        String token = shortToken();
        assertThrows(IllegalArgumentException.class, () ->
                authManager.register("", "blank_name_" + token,
                        "blank_name_" + token + EMAIL_DOMAIN, "pass", "pass", "BIDDER")
        );
    }

    @Test
    void registerThrowsWhenUsernameIsBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                authManager.register("Name", "",
                        "blankuser_" + shortToken() + EMAIL_DOMAIN, "pass", "pass", "BIDDER")
        );
    }

    @Test
    void registerThrowsWhenPasswordIsBlank() {
        String token = shortToken();
        assertThrows(IllegalArgumentException.class, () ->
                authManager.register("Name", "blankpass_" + token,
                        "blankpass_" + token + EMAIL_DOMAIN, "", "", "BIDDER")
        );
    }

    @Test
    void registerThrowsOnDuplicateUsername() {
        String token = shortToken();
        String username = "dupuser_" + token;
        authManager.register("Name1", username,
                username + "1" + EMAIL_DOMAIN, "pass", "pass", "BIDDER");

        assertThrows(IllegalArgumentException.class, () ->
                authManager.register("Name2", username,
                        username + "2" + EMAIL_DOMAIN, "pass", "pass", "BIDDER")
        );
    }

    @Test
    void registerThrowsOnDuplicateEmail() {
        String token = shortToken();
        String email = "dupemail_" + token + EMAIL_DOMAIN;
        authManager.register("Name1", "user1_" + token, email, "pass", "pass", "BIDDER");

        assertThrows(IllegalArgumentException.class, () ->
                authManager.register("Name2", "user2_" + token, email, "pass", "pass", "BIDDER")
        );
    }

    @Test
    void registerThrowsOnInvalidRole() {
        String token = shortToken();
        assertThrows(IllegalArgumentException.class, () ->
                authManager.register("Name", "inv_" + token,
                        "inv_" + token + EMAIL_DOMAIN, "pass", "pass", "HACKER")
        );
    }

    @Test
    void loginSucceedsWithCorrectCredentials() {
        String token = shortToken();
        String username = "loginok_" + token;
        authManager.register("Login User", username,
                username + EMAIL_DOMAIN, "mysecret", "mysecret", "BIDDER");

        Optional<User> result = authManager.login(username, "mysecret");
        assertTrue(result.isPresent());
        assertEquals(username, result.get().getUserName());
    }

    @Test
    void loginFailsWithWrongPassword() {
        String token = shortToken();
        String username = "loginfail_" + token;
        authManager.register("User", username,
                username + EMAIL_DOMAIN, "correct", "correct", "BIDDER");

        assertTrue(authManager.login(username, "wrong").isEmpty());
    }

    @Test
    void loginFailsForNonexistentUser() {
        assertTrue(authManager.login("definitely_not_exists_xyzabc", "anypass").isEmpty());
    }

    @Test
    void loginThrowsWhenUsernameIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> authManager.login("", "pass"));
    }

    @Test
    void loginThrowsWhenPasswordIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> authManager.login("someuser", ""));
    }

    @Test
    void existsByUsernameReturnsTrueForExistingUser() {
        String token = shortToken();
        String username = "exists_" + token;
        authManager.register("Exists", username,
                username + EMAIL_DOMAIN, "pass", "pass", "BIDDER");
        assertTrue(authManager.existsByUsername(username));
    }

    @Test
    void existsByUsernameReturnsFalseForMissingUser() {
        assertFalse(authManager.existsByUsername("missing_xyz_" + shortToken()));
    }

    @Test
    void existsByUsernameReturnsFalseForBlank() {
        assertFalse(authManager.existsByUsername(""));
        assertFalse(authManager.existsByUsername(null));
    }

    @Test
    void existsByEmailReturnsTrueForRegisteredEmail() {
        String token = shortToken();
        String email = "emailok_" + token + EMAIL_DOMAIN;
        authManager.register("Email User", "emailok_" + token, email, "pass", "pass", "BIDDER");
        assertTrue(authManager.existsByEmail(email));
    }

    @Test
    void existsByEmailReturnsFalseForBlank() {
        assertFalse(authManager.existsByEmail(""));
        assertFalse(authManager.existsByEmail(null));
    }

    @Test
    void findByIdReturnsUserWhenExists() {
        String token = shortToken();
        String username = "findme_" + token;
        User created = authManager.register("Find Me", username,
                username + EMAIL_DOMAIN, "pass", "pass", "BIDDER");

        Optional<User> found = authManager.findById(created.getId());
        assertTrue(found.isPresent());
        assertEquals(created.getId(), found.get().getId());
        assertEquals(username, found.get().getUserName());
    }

    @Test
    void findByIdReturnsEmptyForBlankOrNull() {
        assertTrue(authManager.findById("").isEmpty());
        assertTrue(authManager.findById(null).isEmpty());
    }

    @Test
    void findByIdReturnsEmptyForNonexistentId() {
        assertTrue(authManager.findById("NONEXISTENT-ID-" + shortToken()).isEmpty());
    }

    @Test
    void getAllUsersReturnsNonNullCollection() {
        assertNotNull(authManager.getAllUsers());
    }

    @Test
    void registerUserSkipsDuplicateId() {
        String token = shortToken();
        Bidder bidder = new Bidder("TEST-REG-" + token, "Name", "regdup_" + token,
                "regdup_" + token + EMAIL_DOMAIN, "pass");
        authManager.registerUser(bidder);
        assertDoesNotThrow(() -> authManager.registerUser(bidder));
    }

    @Test
    void registerUserThrowsOnNullUser() {
        assertThrows(IllegalArgumentException.class, () -> authManager.registerUser(null));
    }

    private void cleanUpTestData() {
        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM users WHERE email LIKE '%" + EMAIL_DOMAIN + "'" +
                     " OR id LIKE 'TEST-REG-%'")) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Cannot clean auth test data", e);
        }
    }

    private String shortToken() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
