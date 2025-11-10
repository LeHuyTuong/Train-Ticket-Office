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

    // ===== THÊM HÀM KIỂM TRA LỄ VÀ PHỤ THU =====
    private static final BigDecimal HOLIDAY_SURCHARGE_RATE = new BigDecimal("1.20");

    private boolean isHoliday(LocalDate date) {
        if (date.getMonth() == Month.JANUARY && date.getDayOfMonth() == 1) return true;
        if (date.getMonth() == Month.APRIL && date.getDayOfMonth() == 30) return true;
        if (date.getMonth() == Month.MAY && date.getDayOfMonth() == 1) return true;
        if (date.getMonth() == Month.SEPTEMBER && date.getDayOfMonth() == 2) return true;
        return false;
    }
    // ==========================================

    // ===== VIẾT LẠI HOÀN TOÀN HÀM NÀY (ĐỂ XỬ LÝ KHỨ HỒI) =====
    @GetMapping("/search")
    public String searchTripsForRoute(@RequestParam("startStationId") Integer startStationId,
                                      @RequestParam("endStationId") Integer endStationId,
                                      @RequestParam("departureDate")
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate,
                                      @RequestParam(value = "returnDate", required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate returnDate,
                                      @RequestParam(value = "isRoundTrip", required = false) Boolean isRoundTrip,
                                      Model model,
                                      HttpSession session) {

        // --- 1. XỬ LÝ NẾU LÀ KHỨ HỒI ---
        if (isRoundTrip != null && isRoundTrip == true) {
            if (returnDate == null) {
                model.addAttribute("errorMessage", "Bạn đã chọn khứ hồi nhưng chưa chọn ngày về.");
                model.addAttribute("allStations", stationService.getAllStations());
                return "customer/Home";
            }

            // 1. Xóa session cũ (nếu có)
            session.removeAttribute("roundTripNextLeg");
            session.removeAttribute("roundTripGroupId"); // Cũng xóa group id cũ

            // 2. Tạo và lưu thông tin chặng về
            RoundTripInfo returnLegInfo = new RoundTripInfo(endStationId, startStationId, returnDate);
            session.setAttribute("roundTripNextLeg", returnLegInfo);

            // (Tìm Lượt Đi)
            Map<String, Object> outboundResults = findOneWayTrips(startStationId, endStationId, departureDate);
            if (outboundResults.containsKey("errorMessage")) {
                model.addAttribute("errorMessage", outboundResults.get("errorMessage"));
                model.addAttribute("allStations", stationService.getAllStations());
                return "customer/Home";
            }
            model.addAttribute("outboundTrips", outboundResults.get("trips"));
            model.addAttribute("outboundVipCounts", outboundResults.get("vipCounts"));
            model.addAttribute("outboundNormalCounts", outboundResults.get("normalCounts"));
            model.addAttribute("outboundMinPrices", outboundResults.get("minPrices"));
            model.addAttribute("startStation", outboundResults.get("startStation"));
            model.addAttribute("endStation", outboundResults.get("endStation"));

            // (Tìm Lượt Về)
            Map<String, Object> inboundResults = findOneWayTrips(endStationId, startStationId, returnDate);
            model.addAttribute("inboundTrips", inboundResults.get("trips"));
            model.addAttribute("inboundVipCounts", inboundResults.get("vipCounts"));
            model.addAttribute("inboundNormalCounts", inboundResults.get("normalCounts"));
            model.addAttribute("inboundMinPrices", inboundResults.get("minPrices"));

            model.addAttribute("departureDate", departureDate);
            model.addAttribute("returnDate", returnDate);

            return "trip/round-trip-results";
        }

        // --- 2. XỬ LÝ MỘT CHIỀU (LOGIC CŨ) ---

        // ===== DI CHUYỂN DÒNG XÓA VÀO ĐÂY =====
        // (Đảm bảo chỉ xóa session khi tìm 1 chiều)
        session.removeAttribute("roundTripNextLeg");
        session.removeAttribute("roundTripGroupId");
        // ======================================

        Map<String, Object> oneWayResults = findOneWayTrips(startStationId, endStationId, departureDate);

        if (oneWayResults.containsKey("errorMessage")) {
            model.addAttribute("errorMessage", oneWayResults.get("errorMessage"));
            model.addAttribute("allStations", stationService.getAllStations());
            return "customer/Home";
        }

        model.addAttribute("availableTrips", oneWayResults.get("trips"));
        model.addAttribute("availableVipCounts", oneWayResults.get("vipCounts"));
        model.addAttribute("availableNormalCounts", oneWayResults.get("normalCounts"));
        model.addAttribute("tripMinPrices", oneWayResults.get("minPrices"));
        model.addAttribute("startStation", oneWayResults.get("startStation"));
        model.addAttribute("endStation", oneWayResults.get("endStation"));

        return "trip/trip-results";
    }

    // (Hàm findOneWayTrips - Chỉ 1 hàm)
    private Map<String, Object> findOneWayTrips(Integer startId, Integer endId, LocalDate date) {
        Map<String, Object> modelData = new HashMap<>();
        List<Route> routeOpt = routeService.findByStartStationIdAndEndStationId(startId, endId);
        Optional<Station> startStationOpt = stationService.findById(startId);
        Optional<Station> endStationOpt = stationService.findById(endId);

        if (routeOpt.isEmpty() || startStationOpt.isEmpty() || endStationOpt.isEmpty()) {
            modelData.put("errorMessage", "Không tìm thấy tuyến đường nào phù hợp.");
            return modelData;
        }

        Station startStation = startStationOpt.get();
        Station endStation = endStationOpt.get();

        List<Trip> availableTrips;
        if (date != null) {
            availableTrips = tripService.findTripsByRouteAndDate(routeOpt.get(0), date);
        } else {
            availableTrips = tripService.findTripsByRoute(routeOpt.get(0));
        }

        if (startStation.getDistanceKm() == null || endStation.getDistanceKm() == null) {
            modelData.put("errorMessage", "Lỗi cấu hình: Ga chưa có thông tin KM.");
            return modelData;
        }
        int distanceKm = Math.abs(endStation.getDistanceKm() - startStation.getDistanceKm());
        if (distanceKm == 0) distanceKm = 20;

        Map<Long, Long> availableVipCounts = new HashMap<>();
        Map<Long, Long> availableNormalCounts = new HashMap<>();
        Map<Long, BigDecimal> tripMinPrices = new HashMap<>();

        for (Trip trip : availableTrips) {
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
        modelData.put("trips", availableTrips);
        modelData.put("vipCounts", availableVipCounts);
        modelData.put("normalCounts", availableNormalCounts);
        modelData.put("minPrices", tripMinPrices);
        modelData.put("startStation", startStation);
        modelData.put("endStation", endStation);

        return modelData;
    }

    // ===== VIẾT LẠI HOÀN TOÀN HÀM NÀY (Logic Bản đồ ghế) =====
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
                    .map(booking -> booking.getSeat().getSeatId()) // SỬA
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

                for (Seat seat : carriage.getSeats()) { // SỬA
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
        model.addAttribute("availableVipCounts", availableVipCounts); // Gửi VIP
        model.addAttribute("availableNormalCounts", availableNormalCounts); // Gửi Normal
        model.addAttribute("tripMinPrices", tripMinPrices);

        return "trip/all-trips";
    }
    // ======================================================

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
