package com.example.trainticketoffice.repository;

import com.example.trainticketoffice.model.Carriage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CarriageRepository extends JpaRepository<Carriage, Long> {
}