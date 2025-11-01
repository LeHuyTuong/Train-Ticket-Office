package com.example.trainticketoffice.service.impl;

import com.example.trainticketoffice.model.Station;
import com.example.trainticketoffice.repository.StationRepository;
import com.example.trainticketoffice.service.StationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StationServiceImpl implements StationService {

    @Autowired
    private StationRepository stationRepository;

    @Override
    public List<Station> getAllStations() {
        return stationRepository.findAll();
    }

    @Override
    public Station getStationById(Integer id) {
        return stationRepository.findById(id).orElse(null);
    }

    @Override
    public Station createStation(Station station) {
        if (stationRepository.existsByCode(station.getCode())) {
            return null;
        }
        return stationRepository.save(station);
    }

    @Override
    public Station updateStation(Integer id, Station station) {
        Station existingStation = stationRepository.findById(id).orElse(null);
        if (existingStation != null) {
            if (!existingStation.getCode().equals(station.getCode()) &&
                    stationRepository.existsByCode(station.getCode())) {
                return null;
            }

            existingStation.setCode(station.getCode());
            existingStation.setName(station.getName());
            existingStation.setCity(station.getCity());
            existingStation.setProvince(station.getProvince());
            existingStation.setKmFromStart(station.getKmFromStart());
            existingStation.setStatus(station.getStatus());

            return stationRepository.save(existingStation);
        }
        return null;
    }

    @Override
    public void deleteStation(Integer id) {
        stationRepository.deleteById(id);
    }

    @Override
    public boolean stationExists(String code) {
        return stationRepository.existsByCode(code);
    }
}