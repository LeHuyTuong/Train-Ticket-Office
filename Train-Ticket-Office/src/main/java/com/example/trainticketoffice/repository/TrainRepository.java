package com.example.trainticketoffice.repository;

import com.example.trainticketoffice.model.Train;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrainRepository extends JpaRepository<Train, Long> {
    Optional<Train> findByCode(String code);
    boolean existsByCode(String code);
}
