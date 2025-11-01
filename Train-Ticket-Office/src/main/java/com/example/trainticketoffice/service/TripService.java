package com.example.trainticketoffice.service;

import com.example.trainticketoffice.model.Trip;

import java.util.List;
import java.util.Optional;

public interface TripService {
    List<Trip> getAllTrips();
    Optional<Trip> getTripById(Long id);
    Trip saveTrip(Trip trip);
    void deleteTrip(Long id);
}
