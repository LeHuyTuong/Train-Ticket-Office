package com.example.trainticketoffice.service;

import com.example.trainticketoffice.model.User;

import java.util.List;

public interface UserService {
    User getUser(String email, String password);
    boolean addUser(User user);
    User findByUserName(String username);
    public List<User> getUser();
    public void deleteUser(int id);
    public void updateUser(int id , User user);
}
