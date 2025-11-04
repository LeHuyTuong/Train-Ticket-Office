package com.example.trainticketoffice.repository;

import com.example.trainticketoffice.model.Booking;
import com.example.trainticketoffice.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByCode(String code);

    // THÊM HÀM NÀY
    List<Ticket> findByBooking(Booking booking);
}