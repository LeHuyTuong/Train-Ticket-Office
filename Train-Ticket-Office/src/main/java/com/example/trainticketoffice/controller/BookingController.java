package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.common.BookingStatus;
import com.example.trainticketoffice.model.*; // Dùng * (Giữ convention của bạn)
import com.example.trainticketoffice.repository.BookingRepository;
import com.example.trainticketoffice.repository.OrderRepository;
import com.example.trainticketoffice.repository.SeatRepository;
import com.example.trainticketoffice.repository.TripRepository;
import com.example.trainticketoffice.service.BookingService;
import com.example.trainticketoffice.service.SeatService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid; // <-- THÊM IMPORT NÀY
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult; // <-- THÊM IMPORT NÀY
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.Month;

@Controller
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final OrderRepository orderRepository;
    private final SeatRepository seatRepository;
    private final SeatService seatService;

    private static final BigDecimal HOLIDAY_SURCHARGE_RATE = new BigDecimal("1.20");

    private boolean isHoliday(LocalDate date) {
        if (date.getMonth() == Month.JANUARY && date.getDayOfMonth() == 1) return true;
        if (date.getMonth() == Month.APRIL && date.getDayOfMonth() == 30) return true;
        if (date.getMonth() == Month.MAY && date.getDayOfMonth() == 1) return true;
        if (date.getMonth() == Month.SEPTEMBER && date.getDayOfMonth() == 2) return true;
        return false;
    }

    @GetMapping("/new")
    public String showCreateForm(@RequestParam("tripId") Long tripId, Model model,
                                 @RequestParam(value = "context", required = false) String context, // <-- THÊM
                                 HttpSession session) {

        User currentUser = (User) session.getAttribute("userLogin");
        if (currentUser == null) return "redirect:/login";
        Optional<Trip> tripOpt = tripRepository.findById(tripId);
        if (tripOpt.isEmpty()) return "redirect:/";
        bookingService.autoCancelExpiredBookingsForTrip(tripId);
        Trip selectedTrip = tripOpt.get();
        Train train = selectedTrip.getTrain();
        List<Carriage> carriages = train.getCarriages();
        List<Seat> allSeatsOnTrain = seatService.getAllSeats().stream()
                .filter(s -> s.getCarriage().getTrain().getId().equals(train.getId()))
                .collect(Collectors.toList());
        Station startStation = selectedTrip.getRoute().getStartStation();
        Station endStation = selectedTrip.getRoute().getEndStation();
        if (startStation.getDistanceKm() == null || endStation.getDistanceKm() == null) {
            model.addAttribute("errorMessage", "Lỗi cấu hình: Ga chưa có thông tin KM.");
            return "customer/Home";
        }
        int distanceKm = Math.abs(endStation.getDistanceKm() - startStation.getDistanceKm());
        if (distanceKm == 0) distanceKm = 20;
        boolean isTripOnHoliday = isHoliday(selectedTrip.getDepartureTime().toLocalDate());
        BigDecimal currentSurchargeRate = isTripOnHoliday ? HOLIDAY_SURCHARGE_RATE : BigDecimal.ONE;
        Map<Long, BigDecimal> seatPrices = new HashMap<>();
        for (Seat seat : allSeatsOnTrain) {
            SeatType seatType = seat.getCarriage().getSeatType();
            if (seatType != null && seatType.getPricePerKm() != null) {
                BigDecimal basePrice = seatType.getPricePerKm().multiply(BigDecimal.valueOf(distanceKm));
                BigDecimal priceWithSurcharge = basePrice.multiply(currentSurchargeRate);
                priceWithSurcharge = priceWithSurcharge.setScale(0, RoundingMode.HALF_UP);
                seatPrices.put(seat.getSeatId(), priceWithSurcharge);
            } else {
                seatPrices.put(seat.getSeatId(), BigDecimal.valueOf(999999));
            }
        }
        List<Booking> allBookingsForTrip = bookingRepository.findAllByTrip_TripIdAndStatusIn(
                tripId, List.of(BookingStatus.BOOKED, BookingStatus.PAID, BookingStatus.COMPLETED)
        );
        List<Long> paidSeatIds = allBookingsForTrip.stream()
                .filter(b -> b.getStatus() == BookingStatus.PAID || b.getStatus() == BookingStatus.COMPLETED)
                .map(booking -> booking.getSeat().getSeatId())
                .collect(Collectors.toList());
        List<Long> pendingSeatIds = allBookingsForTrip.stream()
                .filter(b -> b.getStatus() == BookingStatus.BOOKED)
                .map(booking -> booking.getSeat().getSeatId())
                .collect(Collectors.toList());
        model.addAttribute("selectedTrip", selectedTrip);
        model.addAttribute("carriages", carriages);
        model.addAttribute("allSeats", allSeatsOnTrain);
        model.addAttribute("seatPrices", seatPrices);
        model.addAttribute("paidSeatIds", paidSeatIds);
        model.addAttribute("pendingSeatIds", pendingSeatIds);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isHoliday", isTripOnHoliday);
        model.addAttribute("surchargeRate", HOLIDAY_SURCHARGE_RATE);
        model.addAttribute("selectedTrip", selectedTrip);
        model.addAttribute("isHoliday", isTripOnHoliday);
        model.addAttribute("surchargeRate", HOLIDAY_SURCHARGE_RATE);
        model.addAttribute("context", context); // <-- THÊM: Truyền context ra view
        return "ticket/form";
    }

    @GetMapping("/passenger-details")
    public String showPassengerDetailsForm(@RequestParam("tripId") Long tripId,
                                           @RequestParam(value = "context", required = false) String context, // <-- THÊM
                                           @RequestParam(value = "seatIds", required = false) List<Long> seatIds,
                                           Model model, HttpSession session,
                                           RedirectAttributes redirectAttributes) {

        User currentUser = (User) session.getAttribute("userLogin");
        if (currentUser == null) return "redirect:/login";

        if (seatIds == null || seatIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn ít nhất một ghế.");
            return "redirect:/bookings/new?tripId=" + tripId;
        }

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chuyến đi."));

        Station startStation = trip.getRoute().getStartStation();
        Station endStation = trip.getRoute().getEndStation();
        int distanceKm = Math.abs(endStation.getDistanceKm() - startStation.getDistanceKm());
        if (distanceKm == 0) distanceKm = 20;
        boolean isTripOnHoliday = isHoliday(trip.getDepartureTime().toLocalDate());
        BigDecimal currentSurchargeRate = isTripOnHoliday ? HOLIDAY_SURCHARGE_RATE : BigDecimal.ONE;

        BookingRequest bookingRequest = new BookingRequest();
        bookingRequest.setTripId(tripId);
        bookingRequest.setContext(context); // <-- THÊM: Gán context vào DTO


        for (Long seatId : seatIds) {
            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new IllegalArgumentException("Ghế không hợp lệ: " + seatId));

            SeatType seatType = seat.getCarriage().getSeatType();
            BigDecimal basePrice = seatType.getPricePerKm().multiply(BigDecimal.valueOf(distanceKm));
            if (isTripOnHoliday) {
                basePrice = basePrice.multiply(currentSurchargeRate);
            }

            PassengerInfo passenger = new PassengerInfo();
            passenger.setSeatId(seat.getSeatId());
            passenger.setSeatNumber(seat.getSeatNumber() + " (Toa " + seat.getCarriage().getName() + ")");
            passenger.setSeatTypeName(seatType.getName());
            passenger.setBasePrice(basePrice.setScale(0, RoundingMode.HALF_UP));

            if (bookingRequest.getPassengers().isEmpty()) {
                passenger.setPassengerName(currentUser.getFullName());
                passenger.setPhone(currentUser.getPhone());
                passenger.setEmail(currentUser.getEmail());
            }

            bookingRequest.getPassengers().add(passenger);
        }

        model.addAttribute("bookingRequest", bookingRequest);
        model.addAttribute("trip", trip);
        return "ticket/passenger-form";
    }

    @PostMapping("/create-order")
    public String createOrder(@Valid @ModelAttribute("bookingRequest") BookingRequest bookingRequest,
                              BindingResult bindingResult, // <-- Thêm cái này
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {

        User currentUser = (User) session.getAttribute("userLogin");
        if (currentUser == null) return "redirect:/login";
        try {
            // 1. Tạo Order như bình thường
            Order createdOrder = bookingService.createOrder(bookingRequest, currentUser);

            // 2. Lấy thông tin khứ hồi
            RoundTripInfo nextLeg = (RoundTripInfo) session.getAttribute("roundTripNextLeg");
            String context = bookingRequest.getContext();
            String groupId = (String) session.getAttribute("roundTripGroupId");

            // 3. Xử lý logic gán Group ID
            if ("outbound".equals(context) && nextLeg != null) {
                // Đây là LƯỢT ĐI của 1 chuyến khứ hồi

                // 3.1. Tạo Group ID mới và lưu vào session
                groupId = UUID.randomUUID().toString();
                session.setAttribute("roundTripGroupId", groupId);

                // 3.2. Gán Group ID cho đơn hàng LƯỢT ĐI
                createdOrder.setRoundTripGroupId(groupId);
                orderRepository.save(createdOrder); // Lưu lại

                // 3.3. Chuyển hướng đến trang chọn LƯỢT VỀ (giữ nguyên logic cũ)
                redirectAttributes.addFlashAttribute("successMessage",
                        "Đã đặt xong lượt đi (Mã ĐH: " + createdOrder.getOrderId() + "). Vui lòng chọn lượt về.");
                String returnUrl = String.format("/trips/search?startStationId=%d&endStationId=%d&departureDate=%s&roundTripFlow=return",
                        nextLeg.getStartStationId(),
                        nextLeg.getEndStationId(),
                        nextLeg.getDepartureDate().toString());
                return "redirect:" + returnUrl;

            } else if ("inbound".equals(context) && groupId != null) {
                // Đây là LƯỢT VỀ của 1 chuyến khứ hồi

                // 3.4. Gán Group ID (lấy từ session) cho đơn hàng LƯỢT VỀ
                createdOrder.setRoundTripGroupId(groupId);
                orderRepository.save(createdOrder); // Lưu lại

                // 3.5. KHÔNG XÓA session , để trang confirm/thanh toán sử dụng
            }

            // 4. Chuyển đến trang xác nhận (cho cả 1 chiều và lượt về)
            redirectAttributes.addFlashAttribute("newOrderId", createdOrder.getOrderId());
            return "redirect:/bookings/confirm";

        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/bookings/new?tripId=" + bookingRequest.getTripId();
        }
    }

    @GetMapping("/confirm")
    public String showConfirmPage(Model model, HttpSession session,
                                  @ModelAttribute("newOrderId") Long newOrderId) {
        User currentUser = (User) session.getAttribute("userLogin");
        if (currentUser == null) return "redirect:/login";

        // Nếu không có newOrderId (ví dụ F5 trang), chuyển về trang vé
        if (newOrderId == null || newOrderId == 0) {
            // Lấy newOrderId từ session nếu có (phòng trường hợp F5)
            Long sessionOrderId = (Long) session.getAttribute("lastNewOrderId");
            if (sessionOrderId != null) {
                newOrderId = sessionOrderId;
            } else {
                return "redirect:/bookings";
            }
        } else {
            // Lưu orderId vào session để nếu F5 trang confirm thì vẫn load được
            session.setAttribute("lastNewOrderId", newOrderId);
        }


        // 1. Lấy đơn hàng VỪA TẠO (là primaryOrder)
        Order justCreatedOrder = orderRepository.findById(newOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        // 2. Lấy GroupId TỪ ĐƠN HÀNG
        String orderGroupId = justCreatedOrder.getRoundTripGroupId();

        List<Order> ordersToShow = new ArrayList<>();
        List<Booking> bookingsToShow = new ArrayList<>();
        Map<Long, String> bookingLegLabels = new HashMap<>();
        BigDecimal totalGroupPrice = BigDecimal.ZERO;

        // 3. Kiểm tra GroupId lấy TỪ ĐƠN HÀNG
        if (orderGroupId != null && !orderGroupId.isBlank()) {
            // --- XỬ LÝ KHỨ HỒI (Gộp đơn) ---

            // 4. Lấy tất cả các đơn hàng trong nhóm và sắp xếp theo thời gian đặt
            ordersToShow = orderRepository.findByRoundTripGroupId(orderGroupId);

            if (ordersToShow.isEmpty()) {
                ordersToShow.add(justCreatedOrder);
            }

            ordersToShow.sort((o1, o2) -> o1.getOrderTime().compareTo(o2.getOrderTime()));

            // 5. Tính tổng tiền, gộp danh sách booking và gán nhãn lượt đi/về
            for (int index = 0; index < ordersToShow.size(); index++) {
                Order order = ordersToShow.get(index);
                totalGroupPrice = totalGroupPrice.add(order.getTotalPrice());
                String legLabel = resolveLegLabel(index);
                for (Booking booking : order.getBookings()) {
                    bookingsToShow.add(booking);
                    bookingLegLabels.put(booking.getBookingId(), legLabel);
                }            }

        } else {
            // --- XỬ LÝ 1 CHIỀU (Như cũ) ---
            ordersToShow.add(justCreatedOrder);
            bookingsToShow.addAll(justCreatedOrder.getBookings());
            totalGroupPrice = justCreatedOrder.getTotalPrice();

            for (Booking booking : bookingsToShow) {
                bookingLegLabels.put(booking.getBookingId(), "Một chiều");
            }
        }

        boolean isRoundTripOrder = orderGroupId != null && !orderGroupId.isBlank() && ordersToShow.size() > 1;

        // 6. Gửi primaryOrderId (là newOrderId) ra view
        model.addAttribute("primaryOrderId", newOrderId);
        model.addAttribute("orders", ordersToShow); // Danh sách các Order (nếu cần)
        model.addAttribute("bookings", bookingsToShow); // Gộp tất cả booking
        model.addAttribute("totalPrice", totalGroupPrice); // Tổng tiền của nhóm
        model.addAttribute("totalTickets", bookingsToShow.size());
        model.addAttribute("isRoundTrip", isRoundTripOrder);
        model.addAttribute("bookingLegLabels", bookingLegLabels);

        return "payment/confirm-payment";
    }

    private String resolveLegLabel(int legIndex) {
        if (legIndex == 0) {
            return "Lượt Đi";
        }
        if (legIndex == 1) {
            return "Lượt Về";
        }
        return "Chặng " + (legIndex + 1);
    }

    @GetMapping
    public String listBookings(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("userLogin");
        if (currentUser == null) return "redirect:/login";
        List<Booking> bookings = bookingService.findAllBookingsByUserId(currentUser.getId());
        model.addAttribute("bookings", bookings);
        return "ticket/list";
    }

    @GetMapping("/{bookingId}")
    public String viewBooking(@PathVariable Long bookingId,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        Optional<Booking> booking = bookingService.findById(bookingId);
        if (booking.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy thông tin đặt vé");
            return "redirect:/bookings";
        }
        model.addAttribute("booking", booking.get());
        return "ticket/detail";
    }

    @GetMapping("/delete/{bookingId}")
    public String deleteBooking(@PathVariable Long bookingId,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("userLogin");
        if (currentUser == null) return "redirect:/login";
        try {
            bookingService.customerCancelBooking(bookingId, currentUser.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy booking " + bookingId + " thành công.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + ex.getMessage());
        }
        return "redirect:/bookings";
    }
}
