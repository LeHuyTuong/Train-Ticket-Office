package com.example.trainticketoffice.service;

import com.example.trainticketoffice.model.Route;
import com.example.trainticketoffice.model.Station;

import java.util.List;

public interface RouteService {

    List<Route> getAllRoutes();

    List<Route> getRoutesByStartAndEndStation(Station startStation, Station endStation);

    Route createRoute(Route route);

    Route updateRoute(Integer id, Route route);

    void deleteRoute(Integer id);

    boolean routeExists(String code);
}