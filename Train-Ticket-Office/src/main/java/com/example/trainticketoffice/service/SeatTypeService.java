package com.example.trainticketoffice.service;

import com.example.trainticketoffice.model.SeatType;
import java.util.List;
import java.util.Optional;

public interface SeatTypeService {
    List<SeatType> getAllSeatTypes();
    Optional<SeatType> getSeatTypeById(Long id);
    SeatType saveSeatType(SeatType seatType);
    void deleteSeatType(Long id);
}