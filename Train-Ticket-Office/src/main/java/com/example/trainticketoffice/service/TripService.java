package com.example.trainticketoffice.service;

import com.example.trainticketoffice.common.TripStatus;
import com.example.trainticketoffice.model.Route;
import com.example.trainticketoffice.model.Trip;
import org.springframework.data.domain.Page; // <-- THÊM

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

    void updateTripStatus(Long tripId, TripStatus newStatus);

    // ===== THÊM HÀM MỚI (CHO ADMIN LIST) =====
    Page<Trip> listAllAdmin(int pageNum, Integer stationId);
}