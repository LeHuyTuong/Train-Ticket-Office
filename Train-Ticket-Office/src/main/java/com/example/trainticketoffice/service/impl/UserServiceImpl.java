package com.example.trainticketoffice.service.impl;

import com.example.trainticketoffice.model.User;
import com.example.trainticketoffice.repository.UserRepository;
import com.example.trainticketoffice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public User getUser(String email, String password) {
        return userRepository.findByEmailAndPassword(email, password);
    }

    @Override
    public boolean addUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            return false; // Nếu tồn tại -> Trả về false
        }
        return userRepository.save(user) != null;
    }

    @Override
    public User findByUserName(String fullName) {
        return userRepository.findByFullName(fullName);
    }

    @Override
    public List<User> getUser() {
        return userRepository.findAll();
    }

    @Override
    public void deleteUser(int id) {
        userRepository.deleteById(id);
    }

    @Override
    public void updateUser(int id, User user) {
        User existUser = userRepository.findById(id).orElse(null);
        if (existUser != null) {
            existUser.setFullName(user.getFullName());
            existUser.setPassword(user.getPassword());
            existUser.setRole(user.getRole());
            userRepository.save(existUser);
        }
    }
}
