package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.common.BookingStatus;
import com.example.trainticketoffice.common.TrainStatus;
import com.example.trainticketoffice.common.TripStatus;
import com.example.trainticketoffice.model.*;
import com.example.trainticketoffice.repository.BookingRepository;
import com.example.trainticketoffice.service.RouteService;
import com.example.trainticketoffice.service.StationService;
import com.example.trainticketoffice.service.TrainService;
import com.example.trainticketoffice.service.TripService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode; // THÊM
import java.time.LocalDate;
import java.time.Month; // THÊM
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/trips")
public class TripController {

    @Autowired
    private TripService tripService;
    @Autowired
    private TrainService trainService;
    @Autowired
    private RouteService routeService;
    @Autowired
    private StationService stationService;
    @Autowired
    private BookingRepository bookingRepository;


    private static final BigDecimal HOLIDAY_SURCHARGE_RATE = new BigDecimal("1.20");

    private boolean isHoliday(LocalDate date) {
        if (date.getMonth() == Month.JANUARY && date.getDayOfMonth() == 1) return true;
        if (date.getMonth() == Month.APRIL && date.getDayOfMonth() == 30) return true;
        if (date.getMonth() == Month.MAY && date.getDayOfMonth() == 1) return true;
        if (date.getMonth() == Month.SEPTEMBER && date.getDayOfMonth() == 2) return true;
        return false;
    }

    @GetMapping("/search")
    public String searchTripsForRoute(@RequestParam("startStationId") Integer startStationId,
                                      @RequestParam("endStationId") Integer endStationId,
                                      @RequestParam(value = "departureDate", required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate,
                                      Model model) {

        List<Route> routeOpt = routeService.findByStartStationIdAndEndStationId(startStationId, endStationId);
        Optional<Station> startStationOpt = stationService.findById(startStationId);
        Optional<Station> endStationOpt = stationService.findById(endStationId);

        if (routeOpt.isEmpty() || startStationOpt.isEmpty() || endStationOpt.isEmpty()) {
            model.addAttribute("errorMessage", "Không tìm thấy tuyến đường nào phù hợp.");
            model.addAttribute("allStations", stationService.getAllStations());
            return "customer/Home";
        }

        Station startStation = startStationOpt.get();
        Station endStation = endStationOpt.get();

        //Lấy danh sách chuyến đi
        List<Trip> availableTrips;
        if (departureDate != null) {
            availableTrips = tripService.findTripsByRouteAndDate(routeOpt.get(0), departureDate);
        } else {
            availableTrips = tripService.findTripsByRoute(routeOpt.get(0));
        }

        // Lấy KM
        if (startStation.getDistanceKm() == null || endStation.getDistanceKm() == null) {
            model.addAttribute("errorMessage", "Lỗi cấu hình: Ga chưa có thông tin KM.");
            return "customer/Home";
        }
        int distanceKm = Math.abs(endStation.getDistanceKm() - startStation.getDistanceKm());
        if (distanceKm == 0) distanceKm = 20;


        Map<Long, Long> availableVipCounts = new HashMap<>();
        Map<Long, Long> availableNormalCounts = new HashMap<>();
        Map<Long, BigDecimal> tripMinPrices = new HashMap<>();

        for (Trip trip : availableTrips) {
            // Kiểm tra Lễ
            boolean isTripOnHoliday = isHoliday(trip.getDepartureTime().toLocalDate());
            BigDecimal currentSurchargeRate = isTripOnHoliday ? HOLIDAY_SURCHARGE_RATE : BigDecimal.ONE;

            //Lấy các Seat ID đã bị đặt
            List<Long> bookedSeatIds = bookingRepository.findAllByTrip_TripIdAndStatusIn(
                            trip.getTripId(),
                            List.of(BookingStatus.BOOKED, BookingStatus.PAID, BookingStatus.COMPLETED)
                    ).stream()
                    .map(booking -> booking.getSeat().getSeatId()) // SỬA: Quay lại getSeat()
                    .collect(Collectors.toList());

            long vipCount = 0;
            long normalCount = 0;
            BigDecimal minPriceInTrip = null;

            //Lặp qua các Toa -> Ghế để đếm và tính giá
            Train train = trip.getTrain();
            for (Carriage carriage : train.getCarriages()) {
                SeatType seatType = carriage.getSeatType();
                if (seatType == null || seatType.getPricePerKm() == null) continue; // Bỏ qua toa lỗi

                // Tính giá cho loại toa này
                BigDecimal basePrice = seatType.getPricePerKm().multiply(BigDecimal.valueOf(distanceKm));
                BigDecimal priceWithSurcharge = basePrice.multiply(currentSurchargeRate);
                BigDecimal finalPrice = priceWithSurcharge.setScale(0, RoundingMode.HALF_UP);

                if (minPriceInTrip == null || finalPrice.compareTo(minPriceInTrip) < 0) {
                    minPriceInTrip = finalPrice;
                }

                // Đếm số ghế trống
                for (Seat seat : carriage.getSeats()) { // Lặp qua các ghế thật
                    if (!bookedSeatIds.contains(seat.getSeatId())) {
                        if (seatType.getName().toLowerCase().contains("vip")) {
                            vipCount++;
                        } else {
                            normalCount++;
                        }
                    }
                }
            }

            availableVipCounts.put(trip.getTripId(), vipCount);
            availableNormalCounts.put(trip.getTripId(), normalCount);
            tripMinPrices.put(trip.getTripId(), minPriceInTrip != null ? minPriceInTrip : BigDecimal.ZERO);
        }

        model.addAttribute("availableTrips", availableTrips);
        model.addAttribute("availableVipCounts", availableVipCounts); // Gửi VIP
        model.addAttribute("availableNormalCounts", availableNormalCounts); // Gửi Normal
        model.addAttribute("tripMinPrices", tripMinPrices);
        model.addAttribute("startStation", startStation);
        model.addAttribute("endStation", endStation);

        return "trip/trip-results";
    }


    @GetMapping("/all")
    public String showAllTrips(Model model) {

        List<Trip> allTrips = tripService.getAllTrips().stream()
                .filter(trip -> trip.getStatus() == TripStatus.UPCOMING || trip.getStatus() == TripStatus.DELAYED)
                .collect(Collectors.toList());

        Map<Long, Long> availableVipCounts = new HashMap<>();
        Map<Long, Long> availableNormalCounts = new HashMap<>();
        Map<Long, BigDecimal> tripMinPrices = new HashMap<>();

        for (Trip trip : allTrips) {
            Station startStation = trip.getRoute().getStartStation();
            Station endStation = trip.getRoute().getEndStation();
            if (startStation.getDistanceKm() == null || endStation.getDistanceKm() == null) continue;

            int distanceKm = Math.abs(endStation.getDistanceKm() - startStation.getDistanceKm());
            if (distanceKm == 0) distanceKm = 20;

            boolean isTripOnHoliday = isHoliday(trip.getDepartureTime().toLocalDate());
            BigDecimal currentSurchargeRate = isTripOnHoliday ? HOLIDAY_SURCHARGE_RATE : BigDecimal.ONE;

            List<Long> bookedSeatIds = bookingRepository.findAllByTrip_TripIdAndStatusIn(
                            trip.getTripId(),
                            List.of(BookingStatus.BOOKED, BookingStatus.PAID, BookingStatus.COMPLETED)
                    ).stream()
                    .map(booking -> booking.getSeat().getSeatId())
                    .collect(Collectors.toList());

            long vipCount = 0;
            long normalCount = 0;
            BigDecimal minPriceInTrip = null;

            Train train = trip.getTrain();
            for (Carriage carriage : train.getCarriages()) {
                SeatType seatType = carriage.getSeatType();
                if (seatType == null || seatType.getPricePerKm() == null) continue;

                BigDecimal basePrice = seatType.getPricePerKm().multiply(BigDecimal.valueOf(distanceKm));
                BigDecimal priceWithSurcharge = basePrice.multiply(currentSurchargeRate);
                BigDecimal finalPrice = priceWithSurcharge.setScale(0, RoundingMode.HALF_UP);

                if (minPriceInTrip == null || finalPrice.compareTo(minPriceInTrip) < 0) {
                    minPriceInTrip = finalPrice;
                }

                for (Seat seat : carriage.getSeats()) {
                    if (!bookedSeatIds.contains(seat.getSeatId())) {
                        if (seatType.getName().toLowerCase().contains("vip")) {
                            vipCount++;
                        } else {
                            normalCount++;
                        }
                    }
                }
            }

            availableVipCounts.put(trip.getTripId(), vipCount);
            availableNormalCounts.put(trip.getTripId(), normalCount);
            tripMinPrices.put(trip.getTripId(), minPriceInTrip != null ? minPriceInTrip : BigDecimal.ZERO);
        }

        model.addAttribute("availableTrips", allTrips);
        model.addAttribute("availableVipCounts", availableVipCounts);
        model.addAttribute("availableNormalCounts", availableNormalCounts);
        model.addAttribute("tripMinPrices", tripMinPrices);

        return "trip/all-trips";
    }


    @GetMapping
    public String listTrips(Model model,
                            @RequestParam(value = "page", defaultValue = "1") int page,
                            @RequestParam(value = "stationId", required = false) Integer stationId) {

        Page<Trip> tripPage = tripService.listAllAdmin(page, stationId);
        List<Trip> trips = tripPage.getContent();
        model.addAttribute("trips", trips);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalItems", tripPage.getTotalElements());
        model.addAttribute("totalPages", tripPage.getTotalPages());
        model.addAttribute("selectedStationId", stationId);
        model.addAttribute("allTripStatus", TripStatus.values());
        model.addAttribute("allStations", stationService.getAllStations());

        return "trip/list";
    }

    private void addCommonAttributes(Model model) {
        List<Train> availableTrains = trainService.getAllTrains().stream()
                .filter(train -> train.getStatus() == TrainStatus.AVAILABLE)
                .collect(Collectors.toList());
        model.addAttribute("allTrains", availableTrains);
        model.addAttribute("allRoutes", routeService.getAllRoutes());
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("trip", new Trip());
        addCommonAttributes(model);
        return "trip/form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Optional<Trip> trip = tripService.getTripById(id);
        if (trip.isPresent()) {
            model.addAttribute("trip", trip.get());
            model.addAttribute("allTrains", trainService.getAllTrains());
            model.addAttribute("allRoutes", routeService.getAllRoutes());
            return "trip/form";
        }
        return "redirect:/trips";
    }

    @PostMapping("/save")
    public String saveTrip(@Valid @ModelAttribute("trip") Trip trip,
                           BindingResult result, Model model,
                           RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            addCommonAttributes(model);
            return "trip/form";
        }
        Optional<Route> routeOpt = routeService.findById(trip.getRoute().getId());
        if (routeOpt.isPresent()) {
            Route route = routeOpt.get();
            trip.setDepartureStation(route.getStartStation().getName());
            trip.setArrivalStation(route.getEndStation().getName());
        } else {
            result.rejectValue("route", "error.route", "Tuyến đường không hợp lệ.");
            addCommonAttributes(model);
            return "trip/form";
        }
        try {
            tripService.saveTrip(trip);
            redirectAttributes.addFlashAttribute("successMessage", "Trip saved successfully!");
            return "redirect:/trips";
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            addCommonAttributes(model);
            return "trip/form";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteTrip(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            tripService.deleteTrip(id);
            redirectAttributes.addFlashAttribute("successMessage", "Trip deleted successfully.");
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa chuyến này. Đã có booking hoặc vé liên quan đến chuyến này.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting trip: " + e.getMessage());
        }
        return "redirect:/trips";
    }

    @PostMapping("/update-status/{id}")
    public String updateTripStatus(@PathVariable("id") Long id,
                                   @RequestParam("status") TripStatus newStatus,
                                   @RequestParam(value = "page", defaultValue = "1") int page,
                                   @RequestParam(value = "stationId", required = false) Integer stationId,
                                   RedirectAttributes redirectAttributes) {
        try {
            tripService.updateTripStatus(id, newStatus);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái chuyến " + id + " thành " + newStatus.getDescription());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }

        String redirectUrl = "/trips?page=" + page;
        if (stationId != null) {
            redirectUrl += "&stationId=" + stationId;
        }
        return "redirect:" + redirectUrl;
    }
}