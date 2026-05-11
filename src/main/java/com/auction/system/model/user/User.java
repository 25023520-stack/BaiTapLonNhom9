package com.auction.system.model.user;

import java.io.Serializable;

public abstract class User implements Serializable {
    private String id;
    private String fullName;
    private String userName;
    private String email;
    private String passWord;

    public User() {}

    public User(String id, String fullName, String userName, String email, String passWord) {
        this.id = id;
        this.fullName = fullName;
        this.userName = userName;
        this.email = email;
        this.passWord = passWord;
    }

    public boolean checkPassword(String password) {
        if (password == null) {
            return false;
        }
        return password.equals(passWord);
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

    public String getEmail() {
        return email;
    }

    public String getPassWord() {
        return passWord;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPassWord(String passWord) {
        this.passWord = passWord;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public abstract String getRole();

    public void setId(String id) {
        this.id = id;
    }
}
