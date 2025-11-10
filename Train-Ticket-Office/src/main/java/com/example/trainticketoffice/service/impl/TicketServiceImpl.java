package com.example.trainticketoffice.service.impl;

import com.example.trainticketoffice.common.TicketStatus;
import com.example.trainticketoffice.model.Booking;
import com.example.trainticketoffice.model.Seat;
import com.example.trainticketoffice.model.Station;
import com.example.trainticketoffice.model.Ticket;
import com.example.trainticketoffice.model.Trip;
import com.example.trainticketoffice.repository.BookingRepository;
import com.example.trainticketoffice.repository.SeatRepository;
import com.example.trainticketoffice.repository.TicketRepository;
import com.example.trainticketoffice.repository.StationRepository;
import com.example.trainticketoffice.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;
    private final StationRepository stationRepository;

    @Autowired
    public TicketServiceImpl(
            TicketRepository ticketRepository,
            BookingRepository bookingRepository,
            SeatRepository seatRepository,
            StationRepository stationRepository
    ) {
        this.ticketRepository = ticketRepository;
        this.bookingRepository = bookingRepository;
        this.seatRepository = seatRepository;
        this.stationRepository = stationRepository;
    }

    @Override
    @Transactional
    public Ticket createTicketForBooking(Booking booking) {
        try {
            Trip trip = booking.getTrip();
            Seat seat = booking.getSeat();

            Optional<Station> fromStationOpt = stationRepository.findById(trip.getRoute().getStartStation().getId());
            Optional<Station> toStationOpt = stationRepository.findById(trip.getRoute().getEndStation().getId());

            if (fromStationOpt.isEmpty() || toStationOpt.isEmpty()) {
                throw new IllegalStateException("Không tìm thấy ga đi hoặc ga đến.");
            }

            Ticket ticket = new Ticket();
            String ticketCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            ticket.setCode(ticketCode);
            ticket.setBooking(booking);
            ticket.setTrip(trip);
            ticket.setFromStation(fromStationOpt.get());
            ticket.setToStation(toStationOpt.get());
            ticket.setSeat(seat);
            ticket.setPassengerName(booking.getPassengerName());
            ticket.setPassengerPhone(booking.getPhone());
            ticket.setPassengerIdCard(booking.getPassengerIdCard());
            ticket.setDob(booking.getDob());
            ticket.setTotalPrice(booking.getPrice());
            ticket.setStatus(TicketStatus.ACTIVE);
            ticket.setBookedAt(LocalDateTime.now());

            return ticketRepository.save(ticket);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    @Transactional
    public String createTicketForBooking(Long bookingId, Map<String, Object> requestData) {
        System.err.println("CẢNH BÁO: Hàm createTicketForBooking(Map) đã lỗi thời và không nên được gọi.");
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isEmpty()) { return null; }
        Ticket newTicket = this.createTicketForBooking(bookingOpt.get());
        return newTicket != null ? newTicket.getCode() : null;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> getTicketIdByCode(String code) {
        return ticketRepository.findByCode(code).map(Ticket::getId);
    }

    @Override
    @Transactional
    public boolean checkInTicket(Long ticketId) {
        Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);
        if (ticketOpt.isEmpty()) { return false; }
        Ticket ticket = ticketOpt.get();
        if (ticket.getStatus() != TicketStatus.ACTIVE) { return false; }
        ticket.setStatus(TicketStatus.CHECKED_IN);
        ticket.setCheckedInAt(LocalDateTime.now());
        ticketRepository.save(ticket);
        return true;
    }

    @Override
    @Transactional
    public boolean cancelTicket(Long ticketId) {
        Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);
        if (ticketOpt.isEmpty()) { return false; }
        Ticket ticket = ticketOpt.get();
        if (ticket.getStatus() == TicketStatus.CANCELLED || ticket.getStatus() == TicketStatus.CHECKED_IN) {
            return false;
        }
        ticket.setStatus(TicketStatus.CANCELLED);
        ticketRepository.save(ticket);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> findAll() {
        return ticketRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Ticket> findById(Long id) {
        return ticketRepository.findById(id);
    }
}