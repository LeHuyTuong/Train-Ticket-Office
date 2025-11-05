package com.example.trainticketoffice.service.impl;

import com.example.trainticketoffice.common.BookingStatus;
import com.example.trainticketoffice.common.SeatStatus;
import com.example.trainticketoffice.model.*;
import com.example.trainticketoffice.repository.*;
import com.example.trainticketoffice.service.BookingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final int HOLD_DURATION_MINUTES = 15;

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public Booking createBooking(Integer userId,
                                 Long tripId,
                                 Long seatId,
                                 String passengerName,
                                 String phone,
                                 String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với mã " + userId));

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chuyến đi với mã " + tripId));

        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ghế với mã " + seatId));

        if (seat.getStatus() == SeatStatus.BOOKED) {
            autoCancelExpiredBookingsForTrip(tripId);
            seat = seatRepository.findById(seatId).get();
            if(seat.getStatus() == SeatStatus.BOOKED) {
                throw new IllegalStateException("Ghế đã được đặt trước đó và vẫn còn thời hạn giữ.");
            }
        }

        boolean hasConflict = bookingRepository.existsByTrip_TripIdAndSeat_SeatIdAndStatusIn(
                trip.getTripId(),
                seat.getSeatId(),
                List.of(BookingStatus.BOOKED, BookingStatus.PAID)
        );

        if (hasConflict) {
            throw new IllegalStateException("Ghế đã được giữ chỗ cho chuyến đi này");
        }

        // ===== SỬA LOGIC TÍNH GIÁ VÉ =====
        BigDecimal finalPrice = seat.getPrice();
        // ===============================

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setTrip(trip);
        booking.setSeat(seat);
        booking.setPassengerName(passengerName);
        booking.setPhone(phone);
        booking.setEmail(email);
        booking.setStatus(BookingStatus.BOOKED);
        booking.setBookingTime(LocalDateTime.now());
        booking.setPrice(finalPrice);

        Booking savedBooking = bookingRepository.save(booking);

        seat.setStatus(SeatStatus.BOOKED);
        seatRepository.save(seat);

        return savedBooking;
    }

    @Override
    public List<Booking> findAllBookings() { return bookingRepository.findAll(); }
    @Override
    public List<Booking> findAllBookingsByUserId(Integer userId) { return bookingRepository.findByUser_Id(userId); }
    @Override
    public Optional<Booking> findById(Long bookingId) { return bookingRepository.findById(bookingId); }

    @Transactional
    private void internalCancelBooking(Booking booking) {
        Seat seat = booking.getSeat();
        if (seat != null) {
            seat.setStatus(SeatStatus.AVAILABLE);
            seatRepository.save(seat);
        }

        List<Ticket> tickets = ticketRepository.findByBooking(booking);
        ticketRepository.deleteAll(tickets);

        List<Payment> payments = paymentRepository.findByBooking(booking);
        paymentRepository.deleteAll(payments);

        bookingRepository.delete(booking);
    }

    @Override
    @Transactional
    public void customerCancelBooking(Long bookingId, Integer userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy booking với ID: " + bookingId));

        if (!booking.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Bạn không có quyền hủy booking này.");
        }

        if (booking.getStatus() == BookingStatus.PAID || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Không thể hủy vé đã thanh toán/hoàn thành. Vui lòng liên hệ quầy vé.");
        }

        internalCancelBooking(booking);
    }

    @Override
    @Transactional
    public void autoCancelExpiredBookingsForTrip(Long tripId) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(HOLD_DURATION_MINUTES);
        List<Booking> booked = bookingRepository.findAllByTrip_TripIdAndStatus(tripId, BookingStatus.BOOKED);

        int cancelCount = 0;
        for (Booking booking : booked) {
            if (booking.getBookingTime().isBefore(cutoffTime)) {
                internalCancelBooking(booking);
                cancelCount++;
            }
        }

        if(cancelCount > 0) {
            System.out.println("SCHEDULER: Đã tự động hủy " + cancelCount + " vé quá hạn cho chuyến " + tripId);
        }
    }
}