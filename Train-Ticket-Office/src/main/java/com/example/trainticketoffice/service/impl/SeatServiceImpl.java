package com.example.trainticketoffice.service.impl;

import com.example.trainticketoffice.model.Seat;
import com.example.trainticketoffice.repository.SeatRepository;
import com.example.trainticketoffice.service.SeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;

    @Autowired
    public SeatServiceImpl(SeatRepository seatRepository) {
        this.seatRepository = seatRepository;
    }

    @Override
    public List<Seat> getAllSeats() {
        return seatRepository.findAll();
    }

    @Override
    public Optional<Seat> getSeatById(Long id) {
        return seatRepository.findById(id);
    }

    @Override
    public Seat saveSeat(Seat seat) {
        // ===== SỬA LỖI Ở ĐÂY =====
        if (seat.getSeatId() == null) {
            // Sửa `seat.getTrain()` thành `seat.getCarriage()`
            // Sửa `existsByTrainAndSeatNumber` thành `existsByCarriageAndSeatNumber`
            if (seatRepository.existsByCarriageAndSeatNumber(seat.getCarriage(), seat.getSeatNumber())) {

                // Sửa thông báo lỗi
                throw new IllegalStateException("Seat number '" + seat.getSeatNumber() + "' already exists on carriage '" + seat.getCarriage().getName() + "'!");
            }
        }
        return seatRepository.save(seat);
        // =========================
    }

    @Override
    public void deleteSeat(Long id) {
        if (!seatRepository.existsById(id)) {
            throw new RuntimeException("Seat not found with ID: " + id);
        }
        seatRepository.deleteById(id);
    }
}