package com.example.trainticketoffice.service;

import com.example.trainticketoffice.model.Booking; // THÊM
import com.example.trainticketoffice.model.Ticket;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TicketService {

    String createTicketForBooking(Long bookingId, Map<String, Object> requestData);
    Ticket createTicketForBooking(Booking booking);
    Optional<Long> getTicketIdByCode(String code);
    boolean checkInTicket(Long ticketId);
    boolean cancelTicket(Long ticketId);
    List<Ticket> findAll();
    Optional<Ticket> findById(Long id);
}