package com.example.trainticketoffice.service;

import com.example.trainticketoffice.model.Booking; // THÊM
import com.example.trainticketoffice.model.Ticket;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TicketService {

    // (Hàm cũ, bị lỗi thời)
    String createTicketForBooking(Long bookingId, Map<String, Object> requestData);

    // ===== HÀM MỚI (DÙNG CHO LOGIC BẢN ĐỒ GHẾ) =====
    /**
     * Tạo 1 vé (Ticket) dựa trên 1 Booking (đặt vé) đã có.
     */
    Ticket createTicketForBooking(Booking booking);
    // ============================================

    Optional<Long> getTicketIdByCode(String code);

    boolean checkInTicket(Long ticketId);

    boolean cancelTicket(Long ticketId);

    List<Ticket> findAll();

    Optional<Ticket> findById(Long id);
}