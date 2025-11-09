package com.example.trainticketoffice.service;

import com.example.trainticketoffice.model.Route;
import com.example.trainticketoffice.model.Station;

import java.util.List;
import java.util.Optional;

public interface RouteService {

    List<Route> getAllRoutes();
    List<Route> getRoutesByStartAndEndStation(Station startStation, Station endStation);
    Route createRoute(Route route);
    Route updateRoute(Integer id, Route route);
    void deleteRoute(Integer id);
    boolean routeExists(String code);
    List<Route> findRouteByStations(Integer startStationId, Integer endStationId);
    List<Route> findByStartStationIdAndEndStationId(Integer startStationId, Integer endStationId);

    // THÊM HÀM NÀY
    Optional<Route> findById(Integer id);

    List<Route> findAllAndFetchStations();

}
