package com.example.trainticketoffice.service;

import com.example.trainticketoffice.model.Booking;

import java.util.List;
import java.util.Optional;

public interface BookingService {
    Booking createBooking(Integer userId,
                          Long tripId,
                          Long seatId,
                          String passengerName,
                          String phone,
                          String email);

    List<Booking> findAllBookings();

    List<Booking> findAllBookingsByUserId(Integer userId);

    Optional<Booking> findById(Long bookingId);

    void customerCancelBooking(Long bookingId, Integer userId);

    // THÊM HÀM NÀY
    void autoCancelExpiredBookingsForTrip(Long tripId);
}