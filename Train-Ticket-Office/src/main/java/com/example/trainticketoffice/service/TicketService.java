package com.example.trainticketoffice.service;

import com.example.trainticketoffice.model.Ticket; // <-- Thêm import

import java.util.List; // <-- Thêm import
import java.util.Map;
import java.util.Optional;

public interface TicketService {

    String createTicketForBooking(Long bookingId, Map<String, Object> requestData);

    Optional<Long> getTicketIdByCode(String code);

    boolean checkInTicket(Long ticketId);

    boolean cancelTicket(Long ticketId);

    // ===== HÀM MỚI (BẮT BUỘC) =====
    /**
     * Lấy tất cả vé (cho trang Admin)
     */
    List<Ticket> findAll();

    /**
     * Tìm 1 vé bằng ID (cho trang chi tiết Admin)
     */
    Optional<Ticket> findById(Long id);
}