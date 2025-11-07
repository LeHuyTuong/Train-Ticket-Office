package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.common.BookingStatus;
import com.example.trainticketoffice.model.*;
import com.example.trainticketoffice.repository.BookingRepository;
import com.example.trainticketoffice.repository.OrderRepository; // THÊM
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
    private final OrderRepository orderRepository; // THÊM

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

        // THÊM: Chạy auto-cancel để giải phóng ghế
        bookingService.autoCancelExpiredBookingsForTrip(tripId);

        Trip selectedTrip = tripOpt.get();
        Train train = selectedTrip.getTrain();

        List<Carriage> carriages = train.getCarriages();

        List<Seat> allSeatsOnTrain = carriages.stream()
                .flatMap(carriage -> carriage.getSeats().stream())
                .collect(Collectors.toList());

        // SỬA: Tách 2 loại ghế
        List<Long> paidSeatIds = bookingRepository.findAllByTrip_TripIdAndStatusIn(
                        tripId,
                        List.of(BookingStatus.PAID, BookingStatus.COMPLETED) // Ghế đã bán
                )
                .stream()
                .map(booking -> booking.getSeat().getSeatId())
                .collect(Collectors.toList());

        List<Long> heldSeatIds = bookingRepository.findAllByTrip_TripIdAndStatusIn(
                        tripId,
                        List.of(BookingStatus.BOOKED) // Ghế đang giữ
                )
                .stream()
                .map(booking -> booking.getSeat().getSeatId())
                .collect(Collectors.toList());

        model.addAttribute("selectedTrip", selectedTrip);
        model.addAttribute("carriages", carriages);
        model.addAttribute("allSeats", allSeatsOnTrain);
        model.addAttribute("paidSeatIds", paidSeatIds); // SỬA
        model.addAttribute("heldSeatIds", heldSeatIds); // THÊM
        model.addAttribute("currentUser", currentUser);

        return "ticket/form";
    }

    @PostMapping
    public String createBooking(HttpSession session,
                                @RequestParam("tripId") Long tripId,
                                @RequestParam("seatIds") List<Long> seatIds,
                                @RequestParam("passengerName") String passengerName,
                                @RequestParam("passengerType") String passengerType, // THÊM
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
            // SỬA: Gọi service mới để tạo Order (giỏ hàng)
            Order createdOrder = bookingService.createOrder(
                    currentUser.getId(),
                    tripId,
                    seatIds,
                    passengerName,
                    passengerType,
                    phone,
                    email
            );

            // SỬA: Chuyển hướng đến trang xác nhận Order
            redirectAttributes.addFlashAttribute("newOrderId", createdOrder.getOrderId());
            return "redirect:/bookings/confirm-order";

        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/bookings/new?tripId=" + tripId;
        }
    }

    // SỬA: Đổi tên hàm và mapping
    @GetMapping("/confirm-order")
    public String showConfirmOrderPage(Model model, HttpSession session,
                                       @ModelAttribute("newOrderId") Long newOrderId,
                                       RedirectAttributes redirectAttributes) {

        User currentUser = (User) session.getAttribute("userLogin");
        if (currentUser == null) {
            return "redirect:/login";
        }

        if (newOrderId == null || newOrderId == 0) {
            // THÊM: Bắt lỗi nếu F5 trang confirm
            redirectAttributes.addFlashAttribute("errorMessage", "Đơn hàng đã được xử lý hoặc đã hết hạn. Vui lòng đặt lại.");
            return "redirect:/";
        }

        Optional<Order> orderOpt = orderRepository.findById(newOrderId);

        if (orderOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy đơn hàng.");
            return "redirect:/bookings";
        }

        Order newOrder = orderOpt.get();

        model.addAttribute("order", newOrder);
        model.addAttribute("bookings", newOrder.getBookings());
        model.addAttribute("totalPrice", newOrder.getTotalPrice());

        return "payment/confirm-order"; // <-- Trang HTML MỚI
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
        // GIỮ NGUYÊN LOGIC GỐC CỦA BẠN (Không kiểm tra session)
        // để đáp ứng yêu cầu "vô thẳng bằng URL"
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
            // SỬA: Nhận % hoàn tiền
            int refundPercent = bookingService.customerCancelBooking(bookingId, currentUser.getId());

            String successMessage = "Đã hủy booking " + bookingId + " thành công.";

            // THÊM: Thông báo hoàn tiền
            if (refundPercent > 0) {
                successMessage += " Bạn sẽ được hoàn lại " + refundPercent + "% số tiền vé.";
            } else if (refundPercent == 0) {
                // Kiểm tra xem vé đã thanh toán chưa
                Optional<Booking> b = bookingService.findById(bookingId);
                if (b.isPresent() && b.get().getStatus() == BookingStatus.CANCELLED && b.get().getOriginalPrice().compareTo(BigDecimal.ZERO) > 0) {
                    successMessage += " Không áp dụng hoàn tiền do đã qua thời gian quy định.";
                }
            }

            redirectAttributes.addFlashAttribute("successMessage", successMessage);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + ex.getMessage());
        }

        return "redirect:/bookings";
    }
}