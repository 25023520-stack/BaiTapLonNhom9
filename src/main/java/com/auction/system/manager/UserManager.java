package com.auction.system.manager;

import com.auction.system.exception.DuplicateUserException;
import com.auction.system.exception.InvalidDataException;
import com.auction.system.exception.AuthenticationException;

import com.auction.system.model.user.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public class UserManager {
    private final Map<String, User> users = new HashMap<>();

    public void register(User user) throws DuplicateUserException, InvalidDataException{
        if (user == null) {
            throw new InvalidDataException("Thông tin người dùng không được để trống");
        }

        if (user.getUserName() == null || user.getUserName().isBlank()) {
            throw new InvalidDataException("Tên đăng nhập không được để trống");
        }

        if (users.containsKey(user.getUserName())) {
            throw new InvalidDataException("Mật khẩu không được để trống");
        }

        users.put(user.getUserName(), user);

    }

    public User login(String userName, String passWord) throws AuthenticationException {
        User user = users.get(userName);

        if (user == null || !user.getPassWord().equals(passWord)) {
            throw new AuthenticationException("Sai tên đăng nhập hoặc mật khẩu" );
        }

        return user;
    }

    public Collection<User> getAllUsers() {
        return users.values();
    }


    
}
