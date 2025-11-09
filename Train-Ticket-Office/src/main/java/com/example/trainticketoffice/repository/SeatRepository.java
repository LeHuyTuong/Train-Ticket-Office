package com.example.trainticketoffice.repository;

import com.example.trainticketoffice.model.Carriage;
import com.example.trainticketoffice.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    /**
     * Kiểm tra xem Số ghế (seatNumber) đã tồn tại trên Toa (carriage) này chưa.
     */
    boolean existsByCarriageAndSeatNumber(Carriage carriage, String seatNumber);
}