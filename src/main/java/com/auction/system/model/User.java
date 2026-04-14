package com.auction.system.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

public abstract class User {
    private String id;
    private String fullName;
    private String userName;
    private String passwordHash;

    public User() {}

    public User(String id, String fullName, String userName, String passWord) {
        this.id = id;
        this.fullName = fullName;
        this.userName = userName;
        setPassWord(passWord);
    }

    public String getFullName() {
        return fullName;
    }

    public String getId() {
        return id;
    }

    public String getUserName() {
        return userName;
    }

    public boolean checkPassword(String rawPassword) {
        return passwordHash != null && passwordHash.equals(hashPassword(rawPassword));
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPassWord(String passWord) {
        this.passwordHash = hashPassword(passWord);
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public abstract String getRole();

    private String hashPassword(String rawPassword) {
        Objects.requireNonNull(rawPassword, "Password must not be null");

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
