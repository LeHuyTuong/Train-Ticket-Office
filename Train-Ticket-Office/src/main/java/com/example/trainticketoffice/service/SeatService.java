package com.example.trainticketoffice.service;

import com.example.trainticketoffice.model.Seat;
import java.util.List;
import java.util.Optional;

public interface SeatService {
    List<Seat> getAllSeats();
    Optional<Seat> getSeatById(Long id);
    Seat saveSeat(Seat seat);
    void deleteSeat(Long id);
}