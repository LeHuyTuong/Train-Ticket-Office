package com.example.trainticketoffice.service.impl;

import com.example.trainticketoffice.model.Station;
import com.example.trainticketoffice.repository.StationRepository;
import com.example.trainticketoffice.service.StationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StationServiceImpl implements StationService {

    // ===== SỐ LƯỢNG GA TRÊN MỖI TRANG (THEO YÊU CẦU CỦA BẠN) =====
    public static final int STATIONS_PER_PAGE = 5;

    @Autowired
    private StationRepository stationRepository;

    @Override
    public List<Station> getAllStations() {
        return stationRepository.findAll();
    }

    // ===== IMPLEMENT PHƯƠNG THỨC MỚI =====
    @Override
    public Page<Station> listAll(int pageNum, String keyword) {
        // Trang bắt đầu từ 0, nên (pageNum - 1)
        Pageable pageable = PageRequest.of(pageNum - 1, STATIONS_PER_PAGE);

        if (keyword != null && !keyword.isEmpty()) {
            // Nếu có từ khóa, gọi hàm tìm kiếm
            return stationRepository.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(keyword, keyword, pageable);
        }

        // Nếu không có từ khóa, phân trang tất cả
        return stationRepository.findAll(pageable);
    }
    // ===================================

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

    @Override
    public Optional<Station> findById(Integer id) {
        return stationRepository.findById(id);
    }
}