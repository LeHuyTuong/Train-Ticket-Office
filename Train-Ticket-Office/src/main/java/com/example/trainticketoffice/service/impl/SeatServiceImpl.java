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
    public Seat createSeat(Seat seat) {
        return seatRepository.save(seat);
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
    public Seat updateSeat(Long id, Seat seatDetails) {
        Seat existingSeat = seatRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khong tim thay seat voi ID: " + id));

        existingSeat.setStatus(seatDetails.getStatus());


        return seatRepository.save(existingSeat);
    }

    @Override
    public void deleteSeat(Long id) {
        if (!seatRepository.existsById(id)) {
            throw new RuntimeException("Khong tim thay seat voi ID: " + id);
        }
        seatRepository.deleteById(id);
    }
}
