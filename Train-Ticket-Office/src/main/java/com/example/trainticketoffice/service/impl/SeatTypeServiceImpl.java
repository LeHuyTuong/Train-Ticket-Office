package com.example.trainticketoffice.service.impl;

import com.example.trainticketoffice.model.SeatType;
import com.example.trainticketoffice.repository.SeatTypeRepository;
import com.example.trainticketoffice.service.SeatTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SeatTypeServiceImpl implements SeatTypeService {

    private final SeatTypeRepository seatTypeRepository;

    @Autowired
    public SeatTypeServiceImpl(SeatTypeRepository seatTypeRepository) {
        this.seatTypeRepository = seatTypeRepository;
    }

    @Override
    public List<SeatType> getAllSeatTypes() {
        return seatTypeRepository.findAll();
    }

    @Override
    public Optional<SeatType> getSeatTypeById(Long id) {
        return seatTypeRepository.findById(id);
    }

    @Override
    public SeatType saveSeatType(SeatType seatType) {
        // (Bạn có thể thêm logic kiểm tra trùng tên ở đây nếu muốn)
        return seatTypeRepository.save(seatType);
    }

    @Override
    public void deleteSeatType(Long id) {
        // (Nên thêm logic kiểm tra xem SeatType này có đang được Toa nào sử dụng không trước khi xóa)
        // (DataIntegrityViolationException sẽ tự động ném ra nếu có Toa đang dùng)
        seatTypeRepository.deleteById(id);
    }
}