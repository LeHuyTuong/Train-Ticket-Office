package com.example.trainticketoffice.service.impl;

import com.example.trainticketoffice.model.Route;
import com.example.trainticketoffice.model.Station;
import com.example.trainticketoffice.repository.RouteRepository;
import com.example.trainticketoffice.service.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RouteServiceImpl implements RouteService {

    @Autowired
    private RouteRepository routeRepository;

    @Override
    public List<Route> getAllRoutes() {
        return routeRepository.findAll();
    }

    @Override
    public List<Route> getRoutesByStartAndEndStation(Station startStation, Station endStation) {
        return routeRepository.findByStartStationAndEndStation(startStation, endStation);
    }

    @Override
    public Route createRoute(Route route) {
        if (routeRepository.existsByCode(route.getCode())) {
            return null; // Route code already exists
        }
        return routeRepository.save(route);
    }

    @Override
    public Route updateRoute(Integer id, Route route) {
        Route existingRoute = routeRepository.findById(id).orElse(null);
        if (existingRoute != null) {

            if (!existingRoute.getCode().equals(route.getCode()) &&
                    routeRepository.existsByCode(route.getCode())) {
                return null;
            }

            existingRoute.setCode(route.getCode());
            existingRoute.setStartStation(route.getStartStation());
            existingRoute.setEndStation(route.getEndStation());
            existingRoute.setTotalDistanceKm(route.getTotalDistanceKm());
            existingRoute.setEstimatedDurationMinutes(route.getEstimatedDurationMinutes());
            existingRoute.setStatus(route.getStatus());

            return routeRepository.save(existingRoute);
        }
        return null;
    }

    @Override
    public void deleteRoute(Integer id) {
        routeRepository.deleteById(id);
    }

    @Override
    public boolean routeExists(String code) {
        return routeRepository.existsByCode(code);
    }

    @Override
    public List<Route> findRouteByStations(Integer startStationId, Integer endStationId) {
        // SỬA DÒNG NÀY:
        // return routeRepository.startStationId(startStationId); // <-- Code CŨ GÂY LỖI
        return routeRepository.findByStartStationIdAndEndStationId(startStationId, endStationId); // <-- Code MỚI ĐÚNG
    }


    @Override
    public List<Route> findByStartStationIdAndEndStationId(Integer startStationId, Integer endStationId) {
        return routeRepository.findByStartStationIdAndEndStationId(startStationId, endStationId);
    }
}