package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.common.BookingStatus;
import com.example.trainticketoffice.common.ResourceNotFoundException; // <-- THÊM
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
    private final OrderRepository orderRepository;


    // ===== HÀM HIỂN THỊ FORM CHỌN GHẾ (ĐÃ REFACTOR) =====
    @GetMapping("/new")
    public String showCreateForm(@RequestParam("tripId") Long tripId, Model model,
                                 @RequestParam(value = "context", required = false) String context,
                                 HttpSession session) {

        User currentUser = (User) session.getAttribute("userLogin");
        if (currentUser == null) return "redirect:/login";

        try {
            // Toàn bộ logic build model được chuyển vào service
            Map<String, Object> modelData = bookingService.getBookingFormDetails(tripId, currentUser);
            model.addAllAttributes(modelData);
            model.addAttribute("context", context); // <-- Truyền context ra view
            return "ticket/form";
        } catch (ResourceNotFoundException e) {
            // (Service ném lỗi nếu không tìm thấy trip)
            return "redirect:/";
        } catch (IllegalStateException e) {
            // (Service ném lỗi nếu ga thiếu KM)
            model.addAttribute("errorMessage", e.getMessage());
            return "customer/Home";
        }
    }

    // ===== HÀM HIỂN THỊ FORM HÀNH KHÁCH (ĐÃ REFACTOR) =====
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

        try {
            // Service chuẩn bị DTO (BookingRequest)
            BookingRequest bookingRequest = bookingService.preparePassengerDetails(tripId, seatIds, currentUser);
            bookingRequest.setContext(context); // <-- Gán context vào DTO

            Trip trip = tripRepository.findById(tripId).get(); // Chỉ lấy trip để hiển thị info

            model.addAttribute("bookingRequest", bookingRequest);
            model.addAttribute("trip", trip);
            return "ticket/passenger-form";

        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/bookings/new?tripId=" + tripId;
        }
    }

    // ===== HÀM TẠO ORDER (ĐÃ REFACTOR) =====
    @PostMapping("/create-order")
    public String createOrder(@Valid @ModelAttribute("bookingRequest") BookingRequest bookingRequest,
                              Model model,
                              BindingResult bindingResult,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {

        User currentUser = (User) session.getAttribute("userLogin");
        if (currentUser == null) return "redirect:/login";

        if (bindingResult.hasErrors()) {
            // Cần reload lại thông tin Trip để trả về view
            Trip trip = tripRepository.findById(bookingRequest.getTripId()).orElse(null);
            model.addAttribute("trip", trip);
            // (Lý tưởng nhất là gọi lại hàm preparePassengerDetails,
            // nhưng làm vậy sẽ mất dữ liệu user đã nhập. Tạm thời trả về view.)
            return "ticket/passenger-form";
        }

        try {
            // Logic của Controller: Quản lý Session và Flow khứ hồi
            RoundTripInfo nextLeg = (RoundTripInfo) session.getAttribute("roundTripNextLeg");
            String context = bookingRequest.getContext();
            String sessionGroupId = (String) session.getAttribute("roundTripGroupId");
            String groupIdToSave = null; // Group ID sẽ được lưu vào DB

            if ("outbound".equals(context) && nextLeg != null) {
                // Đây là LƯỢT ĐI của 1 chuyến khứ hồi
                // 1. Tạo Group ID mới và lưu vào session
                sessionGroupId = UUID.randomUUID().toString();
                session.setAttribute("roundTripGroupId", sessionGroupId);
                groupIdToSave = sessionGroupId;

            } else if ("inbound".equals(context) && sessionGroupId != null) {
                // Đây là LƯỢT VỀ của 1 chuyến khứ hồi
                // 2. Gán Group ID (lấy từ session)
                groupIdToSave = sessionGroupId;
            }
            // (Nếu là vé 1 chiều, groupIdToSave sẽ là null)


            // 3. Gọi Service để tạo Order (Service chỉ làm logic DB)
            Order createdOrder = bookingService.createOrder(bookingRequest, currentUser, groupIdToSave);


            // 4. Xử lý điều hướng (Logic của Controller)
            if ("outbound".equals(context) && nextLeg != null) {
                // 4.1. Chuyển hướng đến trang chọn LƯỢT VỀ
                redirectAttributes.addFlashAttribute("successMessage",
                        "Đã đặt xong lượt đi (Mã ĐH: " + createdOrder.getOrderId() + "). Vui lòng chọn lượt về.");
                String returnUrl = String.format("/trips/search?startStationId=%d&endStationId=%d&departureDate=%s&roundTripFlow=return",
                        nextLeg.getStartStationId(),
                        nextLeg.getEndStationId(),
                        nextLeg.getDepartureDate().toString());
                return "redirect:" + returnUrl;
            }

            // 4.2. Chuyển đến trang xác nhận (cho cả 1 chiều và lượt về)
            redirectAttributes.addFlashAttribute("newOrderId", createdOrder.getOrderId());
            // Xóa session khứ hồi sau khi đã hoàn tất (cho lượt về)
            if ("inbound".equals(context)) {
                session.removeAttribute("roundTripNextLeg");
                session.removeAttribute("roundTripGroupId");
            }

            return "redirect:/bookings/confirm";

        } catch (IllegalArgumentException | IllegalStateException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            // Trả về trang chọn ghế của chuyến đi này
            return "redirect:/bookings/new?tripId=" + bookingRequest.getTripId();
        }
    }

    // ===== HÀM HIỂN THỊ TRANG CONFIRM (ĐÃ REFACTOR) =====
    @GetMapping("/confirm")
    public String showConfirmPage(Model model, HttpSession session,
                                  RedirectAttributes redirectAttributes,
                                  @ModelAttribute("newOrderId") Long newOrderId) {
        User currentUser = (User) session.getAttribute("userLogin");
        if (currentUser == null) return "redirect:/login";

        Long orderIdToLoad = newOrderId;

        if (orderIdToLoad == null || orderIdToLoad == 0) {
            orderIdToLoad = (Long) session.getAttribute("lastNewOrderId");
            if (orderIdToLoad == null) {
                return "redirect:/bookings"; // Không có order nào, về trang vé
            }
        } else {
            session.setAttribute("lastNewOrderId", orderIdToLoad);
        }

        try {
            // Toàn bộ logic gộp vé, tính tổng... được chuyển vào service
            Map<String, Object> modelData = bookingService.getConfirmationDetails(orderIdToLoad, currentUser);
            model.addAllAttributes(modelData);

            // Xóa session ID tạm sau khi đã load thành công
            // session.removeAttribute("lastNewOrderId"); // (Để lại phòng F5)

            return "payment/confirm-payment";

        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/bookings";
        }
    }

    // (Hàm này đã OK - Giữ nguyên)
    private String resolveLegLabel(int legIndex) {
        if (legIndex == 0) {
            return "Lượt Đi";
        }
        if (legIndex == 1) {
            return "Lượt Về";
        }
        return "Chặng " + (legIndex + 1);
    }

    // ===== CÁC HÀM CÒN LẠI (GIỮ NGUYÊN) =====
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
