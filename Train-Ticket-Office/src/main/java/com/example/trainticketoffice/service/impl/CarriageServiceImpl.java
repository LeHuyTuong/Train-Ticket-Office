package com.example.trainticketoffice.service.impl;

import com.example.trainticketoffice.model.Carriage;
import com.example.trainticketoffice.repository.CarriageRepository;
import com.example.trainticketoffice.service.CarriageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CarriageServiceImpl implements CarriageService {

    private final CarriageRepository carriageRepository;

    @Autowired
    public CarriageServiceImpl(CarriageRepository carriageRepository) {
        this.carriageRepository = carriageRepository;
    }

    @Override
    public List<Carriage> getAllCarriages() {
        return carriageRepository.findAll();
    }

    @Override
    public Optional<Carriage> getCarriageById(Long id) {
        return carriageRepository.findById(id);
    }

    @Override
    public Carriage saveCarriage(Carriage carriage) {
        // (Có thể thêm logic kiểm tra trùng tên/vị trí ở đây nếu muốn)
        return carriageRepository.save(carriage);
    }

    @Override
    public void deleteCarriage(Long id) {
        if (!carriageRepository.existsById(id)) {
            throw new RuntimeException("Carriage not found with ID: " + id);
        }
        carriageRepository.deleteById(id);
    }
}