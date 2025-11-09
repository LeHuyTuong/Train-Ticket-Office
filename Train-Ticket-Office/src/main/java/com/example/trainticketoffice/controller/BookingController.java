package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.common.BookingStatus;
import com.example.trainticketoffice.model.*;
import com.example.trainticketoffice.repository.BookingRepository;
import com.example.trainticketoffice.repository.OrderRepository;
import com.example.trainticketoffice.repository.TripRepository;
import com.example.trainticketoffice.service.BookingService;
import com.example.trainticketoffice.service.SeatService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final TripRepository tripRepository;
    private final SeatService seatService;
    private final BookingRepository bookingRepository;
    private final OrderRepository orderRepository;

    // ===== SỬA HÀM NÀY =====
    @GetMapping("/new")
    public String showCreateForm(@RequestParam("tripId") Long tripId, Model model,
                                 HttpSession session) {

        User currentUser = (User) session.getAttribute("userLogin");
        if (currentUser == null) {
            return "redirect:/login";
        }

        Optional<Trip> tripOpt = tripRepository.findById(tripId);
        if (tripOpt.isEmpty()) {
            return "redirect:/";
        }

        // 1. Chạy logic dọn dẹp 15 phút (ĐÃ CÓ)
        bookingService.autoCancelExpiredBookingsForTrip(tripId);

        Trip selectedTrip = tripOpt.get();
        Train train = selectedTrip.getTrain();
        List<Carriage> carriages = train.getCarriages();
        List<Seat> allSeatsOnTrain = carriages.stream()
                .flatMap(carriage -> carriage.getSeats().stream())
                .collect(Collectors.toList());

        // 2. Lấy TẤT CẢ booking (chờ, đã trả, hoàn thành)
        List<Booking> allBookingsForTrip = bookingRepository.findAllByTrip_TripIdAndStatusIn(
                tripId,
                List.of(BookingStatus.BOOKED, BookingStatus.PAID, BookingStatus.COMPLETED)
        );

        // 3. Lọc ra 2 danh sách ID riêng biệt
        // (Ghế đã thanh toán hoặc hoàn thành -> Màu Xám)
        List<Long> paidSeatIds = allBookingsForTrip.stream()
                .filter(b -> b.getStatus() == BookingStatus.PAID || b.getStatus() == BookingStatus.COMPLETED)
                .map(booking -> booking.getSeat().getSeatId())
                .collect(Collectors.toList());

        // (Ghế đang chờ thanh toán -> Màu Vàng)
        List<Long> pendingSeatIds = allBookingsForTrip.stream()
                .filter(b -> b.getStatus() == BookingStatus.BOOKED)
                .map(booking -> booking.getSeat().getSeatId())
                .collect(Collectors.toList());

        model.addAttribute("selectedTrip", selectedTrip);
        model.addAttribute("carriages", carriages);
        model.addAttribute("allSeats", allSeatsOnTrain);

        // 4. Gửi 2 danh sách mới ra view
        model.addAttribute("paidSeatIds", paidSeatIds);
        model.addAttribute("pendingSeatIds", pendingSeatIds);

        model.addAttribute("currentUser", currentUser);

        return "ticket/form";
    }
    // ========================

    @PostMapping
    public String createBooking(HttpSession session,
                                @RequestParam("tripId") Long tripId,
                                @RequestParam("seatIds") List<Long> seatIds,
                                @RequestParam("passengerName") String passengerName,
                                @RequestParam("passengerType") String passengerType,
                                @RequestParam(value = "phone", required = false) String phone,
                                @RequestParam(value = "email", required = false) String email,
                                RedirectAttributes redirectAttributes) {

        User currentUser = (User) session.getAttribute("userLogin");
        if (currentUser == null) {
            return "redirect:/login";
        }

        if (seatIds == null || seatIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn ít nhất một ghế.");
            return "redirect:/bookings/new?tripId=" + tripId;
        }

        try {
            // Gọi hàm createOrder (7 tham số)
            Order createdOrder = bookingService.createOrder(
                    currentUser.getId(),
                    tripId,
                    seatIds,
                    passengerName,
                    passengerType,
                    phone,
                    email
            );

            redirectAttributes.addFlashAttribute("newOrderId", createdOrder.getOrderId());
            return "redirect:/bookings/confirm";

        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/bookings/new?tripId=" + tripId;
        }
    }

    @GetMapping("/confirm")
    public String showConfirmPage(Model model, HttpSession session,
                                  @ModelAttribute("newOrderId") Long newOrderId) {

        User currentUser = (User) session.getAttribute("userLogin");
        if (currentUser == null) {
            return "redirect:/login";
        }

        if (newOrderId == null || newOrderId == 0) {
            return "redirect:/bookings";
        }

        Order newOrder = orderRepository.findById(newOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        model.addAttribute("order", newOrder);
        model.addAttribute("bookings", newOrder.getBookings());
        model.addAttribute("totalPrice", newOrder.getTotalPrice());

        return "payment/confirm-payment";
    }

    @GetMapping
    public String listBookings(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("userLogin");
        if (currentUser == null) {
            return "redirect:/login";
        }

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
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            bookingService.customerCancelBooking(bookingId, currentUser.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy booking " + bookingId + " thành công. Ghế đã được giải phóng.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + ex.getMessage());
        }

        return "redirect:/bookings";
    }
}