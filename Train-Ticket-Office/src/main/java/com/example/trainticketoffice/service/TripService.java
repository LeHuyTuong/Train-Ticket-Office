package com.example.trainticketoffice.service;

import com.example.trainticketoffice.common.TripStatus; // <-- THÊM
import com.example.trainticketoffice.model.Route;
import com.example.trainticketoffice.model.Trip;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TripService {
    List<Trip> getAllTrips();
    Optional<Trip> getTripById(Long id);
    Trip saveTrip(Trip trip);
    void deleteTrip(Long id);
    List<Trip> findTripsByRoute(Route route);
    List<Trip> findTripsByRouteAndDate(Route route, LocalDate departureDate);

    // THÊM HÀM NÀY
    void updateTripStatus(Long tripId, TripStatus newStatus);
}