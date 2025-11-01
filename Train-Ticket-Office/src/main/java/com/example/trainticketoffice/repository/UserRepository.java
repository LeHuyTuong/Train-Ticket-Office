package com.example.trainticketoffice.repository;

import com.example.trainticketoffice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends JpaRepository<User,Integer> {
    User findByEmailAndPassword(String email, String password);
    User findByFullName(String fullName);
}
