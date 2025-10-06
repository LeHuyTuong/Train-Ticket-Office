package com.example.trainticketoffice.repository;

import com.example.trainticketoffice.model.Train;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrainRepository extends JpaRepository<Train, Integer> {

    // Spring Data JPA sẽ tự động tạo một câu query để tìm Train theo 'name'
    // Rất hữu ích nếu bạn cần tìm kiếm
    Optional<Train> findByName(String name);
}
