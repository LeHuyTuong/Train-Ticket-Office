package com.example.trainticketoffice.repository;

import com.example.trainticketoffice.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking,Long> {
}
