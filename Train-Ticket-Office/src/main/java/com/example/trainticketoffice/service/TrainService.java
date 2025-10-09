package com.example.trainticketoffice.service;

import com.example.trainticketoffice.model.Train;

import java.util.List;
import java.util.Optional;

public interface TrainService {

    // Create
    Train createTrain(Train train);

    // Read
    List<Train> getAllTrains();
    Optional<Train> getTrainById(int trainId);

    // Update
    Train updateTrain(int trainId, Train trainDetails);

    // Delete
    void deleteTrain(int trainId);
}
