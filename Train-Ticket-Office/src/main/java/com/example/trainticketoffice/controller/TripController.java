package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.model.Route;
import com.example.trainticketoffice.model.Station;
import com.example.trainticketoffice.model.Trip;
import com.example.trainticketoffice.service.RouteService;
import com.example.trainticketoffice.service.StationService;
import com.example.trainticketoffice.service.TrainService;
import com.example.trainticketoffice.service.TripService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/trips")
public class TripController {

    @Autowired
    private TripService tripService;
    @Autowired
    private TrainService trainService;
    @Autowired
    private RouteService routeService;

    // ===== BẮT BUỘC THÊM SERVICE NÀY =====
    @Autowired
    private StationService stationService;

    // ----------------------------------------------------------------
    // ===== HÀM MỚI (Trang 2 Customer) - SỬA LỖI 404 =====
    // ----------------------------------------------------------------
    /**
     * Xử lý Trang 2 (Kết quả tìm kiếm)
     * Đây là hàm xử lý GET /trips/search
     */
    @GetMapping("/search")
    public String searchTripsForRoute(@RequestParam("startStationId") Integer startStationId,
                                      @RequestParam("endStationId") Integer endStationId,
                                      Model model) {

        // (LƯU Ý: Bạn cần implement logic findRouteByStations và findTripsByRoute trong Service)
        List<Route> routeOpt = routeService.findRouteByStations(startStationId, endStationId);
        Optional<Station> startStationOpt = stationService.findById(startStationId);
        Optional<Station> endStationOpt = stationService.findById(endStationId);

        if (routeOpt.isEmpty() || startStationOpt.isEmpty() || endStationOpt.isEmpty()) {
            model.addAttribute("errorMessage", "Không tìm thấy tuyến đường nào phù hợp.");
            model.addAttribute("allStations", stationService.getAllStations());
            return "customer/home"; // Quay lại Trang 1 (Trang chủ)
        }

        List<Trip> availableTrips = tripService.findTripsByRoute(routeOpt.get(0));

        model.addAttribute("availableTrips", availableTrips);
        model.addAttribute("startStation", startStationOpt.get());
        model.addAttribute("endStation", endStationOpt.get());

        return "customer/trip-results"; // <-- Trả về Trang 2 (Code FE ở dưới)
    }

    // ----------------------------------------------------------------
    // ===== CÁC HÀM CŨ (Trang Admin/Staff) - GIỮ NGUYÊN =====
    // ----------------------------------------------------------------

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