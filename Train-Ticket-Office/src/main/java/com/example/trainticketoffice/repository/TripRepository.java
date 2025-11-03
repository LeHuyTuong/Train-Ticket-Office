package com.example.trainticketoffice.repository;

import com.example.trainticketoffice.model.Route;
import com.example.trainticketoffice.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate; // <-- THÊM IMPORT
import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findAllByRoute(Route route);

    // ===== THÊM HÀM NÀY =====
    List<Trip> findAllByRouteAndDepartureTime(Route route, LocalDate departureTime);
}