package com.example.trainticketoffice.repository;

import com.example.trainticketoffice.common.RefundStatus;
import com.example.trainticketoffice.model.RefundRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {

    // Tìm các yêu cầu đang chờ duyệt
    List<RefundRequest> findByStatus(RefundStatus status);

    // Kiểm tra xem 1 booking đã yêu cầu hoàn tiền chưa
    boolean existsByBooking_BookingId(Long bookingId);
}