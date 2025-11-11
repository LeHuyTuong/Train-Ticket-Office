package com.example.trainticketoffice.repository;

import com.example.trainticketoffice.common.RefundStatus;
import com.example.trainticketoffice.model.RefundRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // <-- THÊM
import org.springframework.data.repository.query.Param; // <-- THÊM
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {

    List<RefundRequest> findByStatus(RefundStatus status);
    boolean existsByBooking_BookingId(Long bookingId);
    @Query("SELECT r FROM RefundRequest r " +
            "JOIN FETCH r.booking b " +
            "JOIN FETCH b.trip t " +
            "JOIN FETCH t.train " +
            "JOIN FETCH b.seat s " +
            "JOIN FETCH s.carriage " +
            "WHERE r.status = :status")
    List<RefundRequest> findByStatusWithDetails(@Param("status") RefundStatus status);
}