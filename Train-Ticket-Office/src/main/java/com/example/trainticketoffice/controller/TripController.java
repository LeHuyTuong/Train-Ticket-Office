package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.common.BookingStatus; // <-- THÊM
import com.example.trainticketoffice.model.*; // <-- SỬA
import com.example.trainticketoffice.repository.BookingRepository; // <-- THÊM
import com.example.trainticketoffice.service.RouteService;
import com.example.trainticketoffice.service.StationService;
import com.example.trainticketoffice.service.TrainService;
import com.example.trainticketoffice.service.TripService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.HashMap; // <-- THÊM
import java.util.List;
import java.util.Map; // <-- THÊM
import java.util.Optional;
import java.util.stream.Collectors; // <-- THÊM

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

    // ===== THÊM REPO NÀY =====
    @Autowired
    private BookingRepository bookingRepository;

    @GetMapping("/search")
    public String searchTripsForRoute(@RequestParam("startStationId") Integer startStationId,
                                      @RequestParam("endStationId") Integer endStationId,
                                      @RequestParam("departureDate")
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

        List<Trip> availableTrips = tripService.findTripsByRouteAndDate(routeOpt.get(0), departureDate);

        // ===== BẮT ĐẦU: LOGIC TÍNH SỐ GHẾ TRỐNG =====
        Map<Long, Long> availableVipCounts = new HashMap<>();
        Map<Long, Long> availableNormalCounts = new HashMap<>();

        for (Trip trip : availableTrips) {
            // 1. Lấy ID của các ghế đã bị đặt cho chuyến đi NÀY
            List<Long> bookedSeatIds = bookingRepository.findAllByTrip_TripIdAndStatusIn(
                            trip.getTripId(),
                            List.of(BookingStatus.BOOKED, BookingStatus.PAID)
                    )
                    .stream()
                    .map(booking -> booking.getSeat().getSeatId())
                    .collect(Collectors.toList());

            // 2. Đi qua tất cả các ghế trên tàu và đếm số ghế CÒN TRỐNG
            long vipCount = 0;
            long normalCount = 0;

            Train train = trip.getTrain();
            for (Carriage carriage : train.getCarriages()) {
                for (Seat seat : carriage.getSeats()) {
                    // Nếu ghế này KHÔNG nằm trong danh sách đã đặt
                    if (!bookedSeatIds.contains(seat.getSeatId())) {
                        if ("VIP".equalsIgnoreCase(seat.getSeatType())) {
                            vipCount++;
                        } else {
                            normalCount++; // Coi tất cả các loại khác là "thường"
                        }
                    }
                }
            }

            // 3. Lưu số lượng đếm được vào Maps
            availableVipCounts.put(trip.getTripId(), vipCount);
            availableNormalCounts.put(trip.getTripId(), normalCount);
        }

        model.addAttribute("availableTrips", availableTrips);
        model.addAttribute("availableVipCounts", availableVipCounts); // <-- GỬI RA VIEW
        model.addAttribute("availableNormalCounts", availableNormalCounts); // <-- GỬI RA VIEW
        // ===== KẾT THÚC: LOGIC TÍNH SỐ GHẾ TRỐNG =====

        model.addAttribute("startStation", startStationOpt.get());
        model.addAttribute("endStation", endStationOpt.get());

        return "trip/trip-results";
    }

    // ... (Toàn bộ các hàm @GetMapping, @PostMapping cho /new, /edit, /save, /delete giữ nguyên như cũ) ...

    @GetMapping
    public String listTrips(Model model) {
        model.addAttribute("trips", tripService.getAllTrips());
        return "trip/list";
    }

    private void addCommonAttributes(Model model) {
        model.addAttribute("allTrains", trainService.getAllTrains());
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
            addCommonAttributes(model);
            return "trip/form";
        }
        return "redirect:/trips";
    }

    @PostMapping("/save")
    public String saveTrip(@Valid @ModelAttribute("trip") Trip trip, BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            addCommonAttributes(model);
            return "trip/form";
        }
        tripService.saveTrip(trip);
        redirectAttributes.addFlashAttribute("successMessage", "Trip saved successfully!");
        return "redirect:/trips";
    }

    @GetMapping("/delete/{id}")
    public String deleteTrip(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            tripService.deleteTrip(id);
            redirectAttributes.addFlashAttribute("successMessage", "Trip deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting trip.");
        }
        return "redirect:/trips";
    }
}