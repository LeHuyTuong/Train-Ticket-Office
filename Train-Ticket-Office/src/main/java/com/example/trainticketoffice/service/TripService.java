package com.example.trainticketoffice.service;

import com.example.trainticketoffice.common.TripStatus;
import com.example.trainticketoffice.model.Route;
import com.example.trainticketoffice.model.Trip;
import org.springframework.data.domain.Page; // <-- THÊM

import java.time.LocalDate;
import java.util.List;
import java.util.Map; // <-- THÊM
import java.util.Optional;

public interface TripService {
    List<Trip> getAllTrips();
    Optional<Trip> getTripById(Long id);
    Trip saveTrip(Trip trip);
    void deleteTrip(Long id);
    List<Trip> findTripsByRoute(Route route);
    List<Trip> findTripsByRouteAndDate(Route route, LocalDate departureDate);
    void updateTripStatus(Long tripId, TripStatus newStatus);
    Page<Trip> listAllAdmin(int pageNum, Integer stationId);

    // ===== LOGIC MỚI ĐƯỢC CHUYỂN TỪ CONTROLLER VÀO =====

    /**
     * Tìm kiếm các chuyến đi một chiều, tính toán giá và số ghế trống.
     * (Logic được chuyển từ TripController.findOneWayTrips)
     *
     * @return Một Map chứa: trips, vipCounts, normalCounts, minPrices, startStation, endStation
     */
    Map<String, Object> findOneWayTrips(Integer startId, Integer endId, LocalDate date);

    /**
     * Lấy tất cả các chuyến đi SẮP DIỄN RA, tính toán giá và số ghế trống.
     * (Logic được chuyển từ TripController.showAllTrips)
     * SỬA LẠI: Thêm tham số pageNum
     *
     * @return Một Map chứa: tripPage, availableVipCounts, availableNormalCounts, tripMinPrices
     */
    Map<String, Object> getAllAvailableTripsForDisplay(int pageNum);
}
