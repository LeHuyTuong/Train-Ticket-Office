package com.example.trainticketoffice.repository;

import com.example.trainticketoffice.model.Seat;
import com.example.trainticketoffice.model.Train;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    boolean existsByTrainAndSeatNumber(Train train, String seatNumber);
}
