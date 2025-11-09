package com.example.trainticketoffice.service;

import com.example.trainticketoffice.model.Booking;
import com.example.trainticketoffice.model.Order;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface BookingService {

    /**
     * SỬA LẠI HÀM NÀY (Quay lại dùng List<SeatId>)
     */
    Order createOrder(Integer userId,
                      Long tripId,
                      List<Long> seatIds, // SỬA
                      String passengerName,
                      String passengerType,
                      String phone,
                      String email);

    // Xóa hàm overloading (6 tham số)
    // Order createOrder(Integer userId, Long tripId, List<Long> seatIds, ...);


    List<Booking> findAllBookings();
    List<Booking> findAllBookingsByUserId(Integer userId);
    Optional<Booking> findById(Long bookingId);
    void customerCancelBooking(Long bookingId, Integer userId);
    void autoCancelExpiredBookingsForTrip(Long tripId);
}