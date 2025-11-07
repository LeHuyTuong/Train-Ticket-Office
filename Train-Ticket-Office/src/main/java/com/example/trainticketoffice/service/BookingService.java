package com.example.trainticketoffice.service;

import com.example.trainticketoffice.model.Booking;
import com.example.trainticketoffice.model.Order; // THÊM

import java.util.List;
import java.util.Optional;

public interface BookingService {
    // SỬA: Thay thế createBooking bằng createOrder
    Order createOrder(Integer userId,
                      Long tripId,
                      List<Long> seatIds,
                      String passengerName,
                      String passengerType, // THÊM
                      String phone,
                      String email);

    List<Booking> findAllBookings();

    List<Booking> findAllBookingsByUserId(Integer userId);

    Optional<Booking> findById(Long bookingId);

    // SỬA: Trả về % hoàn tiền
    int customerCancelBooking(Long bookingId, Integer userId);

    void autoCancelExpiredBookingsForTrip(Long tripId);
}