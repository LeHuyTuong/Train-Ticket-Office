package com.example.trainticketoffice.service.impl;

import com.example.trainticketoffice.model.Booking;
import com.example.trainticketoffice.service.BookingService;

import java.util.List;

public class BookingServiceImpl implements BookingService {
    @Override
    public Booking createBooking(Booking booking) {
        return null;
    }

    @Override
    public List<Booking> findAllBookings() {
        return List.of();
    }

    @Override
    public List<Booking> findAllBookingsByUserId(Long userId) {
        return List.of();
    }
}
