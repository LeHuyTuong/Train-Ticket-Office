package com.example.trainticketoffice.service.impl;

import com.example.trainticketoffice.common.BookingStatus;
import com.example.trainticketoffice.common.SeatStatus;
import com.example.trainticketoffice.model.Booking;
import com.example.trainticketoffice.model.Seat;
import com.example.trainticketoffice.model.Trip;
import com.example.trainticketoffice.model.User;
import com.example.trainticketoffice.repository.BookingRepository;
import com.example.trainticketoffice.repository.SeatRepository;
import com.example.trainticketoffice.repository.TripRepository;
import com.example.trainticketoffice.repository.UserRepository;
import com.example.trainticketoffice.service.BookingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final SeatRepository seatRepository;

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
            throw new IllegalStateException("Ghế đã được đặt trước đó");
        }

        boolean hasConflict = bookingRepository.existsByTrip_TripIdAndSeat_SeatIdAndStatusIn(
                trip.getTripId(),
                seat.getSeatId(),
                List.of(BookingStatus.BOOKED, BookingStatus.PAID)
        );

        if (hasConflict) {
            throw new IllegalStateException("Ghế đã được giữ chỗ cho chuyến đi này");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setTrip(trip);
        booking.setSeat(seat);
        booking.setPassengerName(passengerName);
        booking.setPhone(phone);
        booking.setEmail(email);
        booking.setStatus(BookingStatus.BOOKED);
        booking.setBookingTime(LocalDate.now());

        Booking savedBooking = bookingRepository.save(booking);

        seat.setStatus(SeatStatus.BOOKED);
        seatRepository.save(seat);

        return savedBooking;
    }

    @Override
    public List<Booking> findAllBookings() {
        return bookingRepository.findAll();
    }

    @Override
    public List<Booking> findAllBookingsByUserId(Integer userId) {
        return bookingRepository.findByUser_Id(userId);
    }

    @Override
    public Optional<Booking> findById(Long bookingId) {
        return bookingRepository.findById(bookingId);
    }
}
