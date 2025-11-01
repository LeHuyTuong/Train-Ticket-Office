package com.example.trainticketoffice.service;

import com.example.trainticketoffice.model.Booking;

import java.util.List;

public interface BookingService {
    Booking createBooking(Booking booking);
    List<Booking> findAllBookings();
    List<Booking> findAllBookingsByUserId(Long userId);
}
