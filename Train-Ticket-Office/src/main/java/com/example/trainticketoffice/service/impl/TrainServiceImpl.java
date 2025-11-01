package com.example.trainticketoffice.service.impl;

import com.example.trainticketoffice.model.Train;
import com.example.trainticketoffice.repository.TrainRepository;
import com.example.trainticketoffice.service.TrainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TrainServiceImpl implements TrainService {

    private final TrainRepository trainRepository;

    @Autowired
    public TrainServiceImpl(TrainRepository trainRepository) {
        this.trainRepository = trainRepository;
    }

    @Override
    public List<Train> getAllTrains() {
        return trainRepository.findAll();
    }

    @Override
    public Optional<Train> getTrainById(Long id) {
        return trainRepository.findById(id);
    }

    @Override
    public Train saveTrain(Train train) {
        if (train.getId() == null) {
            if (trainRepository.existsByCode(train.getCode())) {
                throw new IllegalStateException("Train code '" + train.getCode() + "' already exists!");
            }
        }
        return trainRepository.save(train);
    }

    @Override
    public void deleteTrain(Long id) {
        if (!trainRepository.existsById(id)) {
            throw new RuntimeException("Train not found with ID: " + id);
        }
        trainRepository.deleteById(id);
    }
}
