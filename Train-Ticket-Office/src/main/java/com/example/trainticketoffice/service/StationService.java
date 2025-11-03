package com.example.trainticketoffice.service;

import com.example.trainticketoffice.model.Station;

import java.util.List;
import java.util.Optional;

public interface StationService {

    List<Station> getAllStations();

    Station getStationById(Integer id);

    Station createStation(Station station);

    Station updateStation(Integer id, Station station);

    void deleteStation(Integer id);

    boolean stationExists(String code);

    Optional<Station> findById(Integer id);
}