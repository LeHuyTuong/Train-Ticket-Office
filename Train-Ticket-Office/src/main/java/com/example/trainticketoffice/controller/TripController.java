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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
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

        Map<Long, Long> availableVipCounts = new HashMap<>();
        Map<Long, Long> availableNormalCounts = new HashMap<>();

        for (Trip trip : availableTrips) {
            List<Long> bookedSeatIds = bookingRepository.findAllByTrip_TripIdAndStatusIn(
                            trip.getTripId(),
                            List.of(BookingStatus.BOOKED, BookingStatus.PAID)
                    )
                    .stream()
                    .map(booking -> booking.getSeat().getSeatId())
                    .collect(Collectors.toList());

            long vipCount = 0;
            long normalCount = 0;

            Train train = trip.getTrain();
            for (Carriage carriage : train.getCarriages()) {
                for (Seat seat : carriage.getSeats()) {
                    if (!bookedSeatIds.contains(seat.getSeatId())) {
                        if ("VIP".equalsIgnoreCase(seat.getSeatType())) {
                            vipCount++;
                        } else {
                            normalCount++;
                        }
                    }
                }
            }

            availableVipCounts.put(trip.getTripId(), vipCount);
            availableNormalCounts.put(trip.getTripId(), normalCount);
        }

        model.addAttribute("availableTrips", availableTrips);
        model.addAttribute("availableVipCounts", availableVipCounts);
        model.addAttribute("availableNormalCounts", availableNormalCounts);

        model.addAttribute("startStation", startStationOpt.get());
        model.addAttribute("endStation", endStationOpt.get());

        return "trip/trip-results";
    }


    @GetMapping
    public String listTrips(Model model) {
        model.addAttribute("trips", tripService.getAllTrips());
        model.addAttribute("allTripStatus", TripStatus.values());
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
        } catch (IllegalStateException e) { // <-- Bắt lỗi mới
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting trip: " + e.getMessage());
        }
        return "redirect:/trips";
    }

    @PostMapping("/update-status/{id}")
    public String updateTripStatus(@PathVariable("id") Long id,
                                   @RequestParam("status") TripStatus newStatus,
                                   RedirectAttributes redirectAttributes) {
        try {
            tripService.updateTripStatus(id, newStatus);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái chuyến " + id + " thành " + newStatus.getDescription());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/trips";
    }
}