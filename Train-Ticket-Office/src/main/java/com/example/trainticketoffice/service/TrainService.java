package com.example.trainticketoffice.service;

import com.example.trainticketoffice.model.Train;

import java.util.List;
import java.util.Optional;

public interface TrainService {
    List<Train> getAllTrains();
    Optional<Train> getTrainById(Long id);
    Train saveTrain(Train train);
    void deleteTrain(Long id);
}
