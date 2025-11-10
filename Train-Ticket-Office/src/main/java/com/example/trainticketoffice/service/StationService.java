package com.example.trainticketoffice.service;

import com.example.trainticketoffice.model.Station;
import org.springframework.data.domain.Page; // <-- THÊM

import java.util.List;
import java.util.Optional;

public interface StationService {

    List<Station> getAllStations(); // (Giữ lại nếu có nơi khác đang dùng)
    Page<Station> listAll(int pageNum, String keyword);
    Station getStationById(Integer id);
    Station createStation(Station station);
    Station updateStation(Integer id, Station station);
    void deleteStation(Integer id);
    boolean stationExists(String code);
    Optional<Station> findById(Integer id);
}