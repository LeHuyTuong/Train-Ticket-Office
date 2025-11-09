package com.example.trainticketoffice.service;

import com.example.trainticketoffice.model.Booking;
import com.example.trainticketoffice.model.BookingRequest; // THÊM
import com.example.trainticketoffice.model.Order;
import com.example.trainticketoffice.model.User; // THÊM

import java.util.List;
import java.util.Optional;

public interface BookingService {

    /**
     * SỬA LẠI HOÀN TOÀN HÀM NÀY
     * Nó sẽ nhận 1 BookingRequest chứa danh sách hành khách
     */
    Order createOrder(BookingRequest bookingRequest, User user);

    // (Xóa các hàm createOrder cũ)

    List<Booking> findAllBookings();
    List<Booking> findAllBookingsByUserId(Integer userId);
    Optional<Booking> findById(Long bookingId);
    void customerCancelBooking(Long bookingId, Integer userId);
    void autoCancelExpiredBookingsForTrip(Long tripId);
}