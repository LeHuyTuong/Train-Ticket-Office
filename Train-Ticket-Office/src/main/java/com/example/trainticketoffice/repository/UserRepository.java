package com.example.trainticketoffice.repository;

import com.example.trainticketoffice.model.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User,Integer> {
}
