package com.example.trainticketoffice.service;

import com.example.trainticketoffice.model.Seat;
import java.util.List;
import java.util.Optional;

public interface SeatService {

    Seat createSeat(Seat seat);

    List<Seat> getAllSeats();

    Optional<Seat> getSeatById(Long id);

    Seat updateSeat(Long id, Seat seatDetails);

    void deleteSeat(Long id);
}
