package com.example.trainticketoffice.repository;

import com.example.trainticketoffice.common.BookingStatus;
import com.example.trainticketoffice.model.Booking;
// import com.example.trainticketoffice.model.Carriage; // XÓA
// import com.example.trainticketoffice.model.Trip; // XÓA
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking,Long> {

    List<Booking> findByUser_Id(Integer userId);
    boolean existsByTrip_TripIdAndSeat_SeatIdAndStatusIn(Long tripId, Long seatId, Collection<BookingStatus> statuses);
    List<Booking> findAllByTrip_TripIdAndStatusIn(Long tripId, Collection<BookingStatus> statuses);
    List<Booking> findAllByTrip_TripIdAndStatus(Long tripId, BookingStatus status);
}