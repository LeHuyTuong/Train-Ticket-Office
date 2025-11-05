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
    public String createTicketForBooking(Long bookingId, Map<String, Object> requestData) {
        // (Hàm này có vẻ là logic cũ, bạn nên xem xét xóa nó
        // và thay bằng một hàm service tạo Ticket trực tiếp từ Booking)
        try {
            Long seatId = ((Number) requestData.get("seatId")).longValue();
            String passengerName = (String) requestData.get("passengerName");
            String passengerPhone = (String) requestData.get("passengerPhone");
            String passengerIdCard = (String) requestData.get("passengerIdCard");

            Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
            if (bookingOpt.isEmpty()) { return null; }
            Booking booking = bookingOpt.get(); // <-- Booking đã chứa giá

            Optional<Seat> seatOpt = seatRepository.findById(seatId);
            if (seatOpt.isEmpty()) { return null; }
            Seat seat = seatOpt.get();

            Trip trip = booking.getTrip();


            Optional<Station> fromStationOpt = stationRepository.findByName(trip.getDepartureStation());
            Optional<Station> toStationOpt = stationRepository.findByName(trip.getArrivalStation());

            if (fromStationOpt.isEmpty() || toStationOpt.isEmpty()) {
                return null;
            }

            Ticket ticket = new Ticket();
            String ticketCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            ticket.setCode(ticketCode);
            ticket.setBooking(booking);
            ticket.setTrip(trip);
            ticket.setFromStation(fromStationOpt.get());
            ticket.setToStation(toStationOpt.get());
            ticket.setSeat(seat);
            ticket.setPassengerName(passengerName);
            ticket.setPassengerPhone(passengerPhone);
            ticket.setPassengerIdCard(passengerIdCard);

            // ===== SỬA LỖI Ở ĐÂY =====
            // 1. Xóa dòng lỗi 'setDistanceKm' vì trường này không còn
            // ticket.setDistanceKm(BigDecimal.valueOf(200.0)); // <-- XÓA DÒNG NÀY

            // 2. Lấy giá chính xác từ Booking (đã được tính khi tạo)
            ticket.setTotalPrice(booking.getPrice()); // <-- SỬA DÒNG NÀY
            // =========================

            ticket.setStatus(TicketStatus.ACTIVE);
            ticket.setBookedAt(LocalDateTime.now());

            ticketRepository.save(ticket);
            return ticketCode;

        } catch (Exception e) {

            return null;
        }
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

        if (ticketOpt.isEmpty()) { return false; } // 404
        Ticket ticket = ticketOpt.get();

        if (ticket.getStatus() != TicketStatus.ACTIVE) { return false; } // 400

        ticket.setStatus(TicketStatus.CHECKED_IN);
        ticket.setCheckedInAt(LocalDateTime.now());
        ticketRepository.save(ticket);
        return true;
    }

    @Override
    @Transactional
    public boolean cancelTicket(Long ticketId) {
        Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);

        if (ticketOpt.isEmpty()) { return false; } // 404
        Ticket ticket = ticketOpt.get();


        if (ticket.getStatus() == TicketStatus.CANCELLED || ticket.getStatus() == TicketStatus.CHECKED_IN) {
            return false; // 400 Bad Request
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