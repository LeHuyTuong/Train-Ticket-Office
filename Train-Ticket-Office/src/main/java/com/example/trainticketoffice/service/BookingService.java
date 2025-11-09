package com.example.trainticketoffice.service;

import com.example.trainticketoffice.model.Booking;
import com.example.trainticketoffice.model.Order;

import java.util.List;
import java.util.Optional;

public interface BookingService {

    /**
     * Phương thức đầy đủ (được gọi bởi DataInitializer).
     */
    Order createOrder(Integer userId,
                      Long tripId,
                      List<Long> seatIds,
                      String passengerName,
                      String passengerType, // Dành cho DataInitializer
                      String phone,
                      String email);

    /**
     * Phương thức rút gọn (được gọi bởi BookingController).
     * (Chúng ta thêm 1 hàm Overloading để cả 2 nơi đều gọi được)
     */
    Order createOrder(Integer userId,
                      Long tripId,
                      List<Long> seatIds,
                      String passengerName,
                      String phone,
                      String email);


    List<Booking> findAllBookings();

    List<Booking> findAllBookingsByUserId(Integer userId);

    Optional<Booking> findById(Long bookingId);

    void customerCancelBooking(Long bookingId, Integer userId);

    void autoCancelExpiredBookingsForTrip(Long tripId);
}