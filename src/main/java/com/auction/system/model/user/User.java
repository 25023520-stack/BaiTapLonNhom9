package com.auction.system.model.user;

public abstract class User {
    private String id;
    private String fullName;
    private String userName;
    private String passWord;

    public User() {}

    public User(String id, String fullName, String userName, String passWord) {
        this.id = id;
        this.fullName = fullName;
        this.userName = userName;
        this.passWord = passWord;
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

    public abstract String getRole(); // xác định vai trò của người dùng (Seller, Bidder, Admin)
}
