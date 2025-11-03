package com.example.trainticketoffice.service.impl;

import com.example.trainticketoffice.model.Route;
import com.example.trainticketoffice.model.Trip;
import com.example.trainticketoffice.repository.TripRepository;
import com.example.trainticketoffice.service.TripService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TripServiceImpl implements TripService {

    @Autowired
    private TripRepository tripRepository;

    @Override
    public List<Trip> getAllTrips() {
        return tripRepository.findAll();
    }

    @Override
    public Optional<Trip> getTripById(Long id) {
        return tripRepository.findById(id);
    }

    @Override
    public Trip saveTrip(Trip trip) {
        return tripRepository.save(trip);
    }

    @Override
    public void deleteTrip(Long id) {
        tripRepository.deleteById(id);
    }

    @Override
    public List<Trip> findTripsByRoute(Route route) {
        // Chúng ta sẽ cần tạo hàm này trong Repository
        return tripRepository.findAllByRoute(route);
    }

    @Override
    public List<Trip> findTripsByRouteAndDate(Route route, LocalDate departureDate) {

        LocalDateTime startTime = departureDate.atStartOfDay(); // 10/11 00:00:00
        LocalDateTime endTime = departureDate.atTime(23, 59, 59); // 10/11 23:59:59

        // Gọi hàm repository mới
        return tripRepository.findAllByRouteAndDepartureTimeBetween(route, startTime, endTime);
    }
}