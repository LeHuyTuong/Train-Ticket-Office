package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.common.TrainStatus;
import com.example.trainticketoffice.common.TripStatus;
import com.example.trainticketoffice.model.*;
import com.example.trainticketoffice.service.RouteService;
import com.example.trainticketoffice.service.StationService;
import com.example.trainticketoffice.service.TrainService;
import com.example.trainticketoffice.service.TripService;
import jakarta.servlet.http.HttpSession;
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

import java.time.LocalDate;
import java.util.*;
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
    @GetMapping("/search")
    public String searchTripsForRoute(@RequestParam("startStationId") Integer startStationId,
                                      @RequestParam("endStationId") Integer endStationId,
                                      @RequestParam("departureDate")
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate,
                                      @RequestParam(value = "returnDate", required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate returnDate,
                                      @RequestParam(value = "isRoundTrip", required = false) Boolean isRoundTrip,
                                      @RequestParam(value = "roundTripFlow", required = false) String roundTripFlow,
                                      @RequestParam(value = "context", required = false) String context,
                                      Model model,
                                      HttpSession session) {

        // --- 1. XỬ LÝ NẾU LÀ KHỨ HỒI (TÌM KIẾM MỚI) ---
        if (isRoundTrip != null && isRoundTrip == true) {
            if (returnDate == null) {
                model.addAttribute("errorMessage", "Bạn đã chọn khứ hồi nhưng chưa chọn ngày về.");
                model.addAttribute("allStations", stationService.getAllStations());
                return "customer/Home";
            }

            session.removeAttribute("roundTripNextLeg");
            session.removeAttribute("roundTripGroupId");

            RoundTripInfo returnLegInfo = new RoundTripInfo(endStationId, startStationId, returnDate);
            session.setAttribute("roundTripNextLeg", returnLegInfo);

            // (Tìm Lượt Đi - Gọi Service)
            Map<String, Object> outboundResults = tripService.findOneWayTrips(startStationId, endStationId, departureDate);
            if (outboundResults.containsKey("errorMessage")) {
                model.addAttribute("errorMessage", outboundResults.get("errorMessage"));
                model.addAttribute("allStations", stationService.getAllStations());
                return "customer/Home";
            }
            // Thêm tất cả kết quả vào model
            model.addAttribute("outboundTrips", outboundResults.get("trips"));
            model.addAttribute("outboundVipCounts", outboundResults.get("vipCounts"));
            model.addAttribute("outboundNormalCounts", outboundResults.get("normalCounts"));
            model.addAttribute("outboundMinPrices", outboundResults.get("minPrices"));
            model.addAttribute("startStation", outboundResults.get("startStation"));
            model.addAttribute("endStation", outboundResults.get("endStation"));

            // (Tìm Lượt Về - Gọi Service)
            Map<String, Object> inboundResults = tripService.findOneWayTrips(endStationId, startStationId, returnDate);
            // Thêm tất cả kết quả vào model
            model.addAttribute("inboundTrips", inboundResults.get("trips"));
            model.addAttribute("inboundVipCounts", inboundResults.get("vipCounts"));
            model.addAttribute("inboundNormalCounts", inboundResults.get("normalCounts"));
            model.addAttribute("inboundMinPrices", inboundResults.get("minPrices"));

            model.addAttribute("departureDate", departureDate);
            model.addAttribute("returnDate", returnDate);

            return "trip/round-trip-results";
        }

        // --- 2. XỬ LÝ MỘT CHIỀU (HOẶC HIỂN THỊ LƯỢT VỀ) ---
        boolean isReturnFlow = roundTripFlow != null && roundTripFlow.equalsIgnoreCase("return");

        RoundTripInfo roundTripNextLeg = (RoundTripInfo) session.getAttribute("roundTripNextLeg");
        boolean matchesSavedReturnLeg = false;
        if (roundTripNextLeg != null) {
            boolean sameRoute = Objects.equals(roundTripNextLeg.getStartStationId(), startStationId)
                    && Objects.equals(roundTripNextLeg.getEndStationId(), endStationId);
            boolean sameDate = roundTripNextLeg.getDepartureDate() == null
                    || Objects.equals(roundTripNextLeg.getDepartureDate(), departureDate);
            matchesSavedReturnLeg = sameRoute && sameDate;
        }

        boolean selectingReturnLeg = isReturnFlow || matchesSavedReturnLeg;

        if (!selectingReturnLeg) {
            session.removeAttribute("roundTripNextLeg");
            session.removeAttribute("roundTripGroupId");
        }

        if (selectingReturnLeg) {
            model.addAttribute("bookingContext", "inbound");
        } else {
            model.addAttribute("bookingContext", context);
        }
        model.addAttribute("selectingReturnLeg", selectingReturnLeg);

        // (Tìm Một Chiều - Gọi Service)
        Map<String, Object> oneWayResults = tripService.findOneWayTrips(startStationId, endStationId, departureDate);

        if (oneWayResults.containsKey("errorMessage")) {
            model.addAttribute("errorMessage", oneWayResults.get("errorMessage"));
            model.addAttribute("allStations", stationService.getAllStations());
            return "customer/Home";
        }

        // Thêm tất cả kết quả vào model
        model.addAttribute("availableTrips", oneWayResults.get("trips"));
        model.addAttribute("availableVipCounts", oneWayResults.get("vipCounts"));
        model.addAttribute("availableNormalCounts", oneWayResults.get("normalCounts"));
        model.addAttribute("tripMinPrices", oneWayResults.get("minPrices"));
        model.addAttribute("startStation", oneWayResults.get("startStation"));
        model.addAttribute("endStation", oneWayResults.get("endStation"));

        return "trip/trip-results";
    }


    // ===== HÀM HIỂN THỊ TẤT CẢ CHUYẾN ĐÃ ĐƯỢC REFACTOR =====
    @GetMapping("/all")
    public String showAllTrips(Model model) {
        // Chỉ cần gọi service, service sẽ trả về 1 map chứa mọi thứ
        Map<String, Object> modelData = tripService.getAllAvailableTripsForDisplay();
        model.addAllAttributes(modelData);
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
