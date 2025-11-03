package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.model.Booking;
import com.example.trainticketoffice.model.Seat;
import com.example.trainticketoffice.model.Trip;
import com.example.trainticketoffice.model.User;
import com.example.trainticketoffice.repository.TripRepository; // Giữ lại
import com.example.trainticketoffice.service.BookingService;
import com.example.trainticketoffice.service.SeatService;
import jakarta.servlet.http.HttpSession; // Thêm import này
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final TripRepository tripRepository;
    private final SeatService seatService;

    /**
     * HÀM MỚI (Trang 3 Customer): Hiển thị form đặt vé
     */
    @GetMapping("/new")
    public String showCreateForm(@RequestParam("tripId") Long tripId, Model model,
                                 HttpSession session) { // Dùng HttpSession

        User currentUser = (User) session.getAttribute("userLogin");
        if (currentUser == null) {
            return "redirect:/login"; // Bắt buộc login
        }

        Optional<Trip> tripOpt = tripRepository.findById(tripId);
        if (tripOpt.isEmpty()) {
            return "redirect:/"; // Nếu tripId bậy, về trang chủ
        }

        // (Bạn cần implement logic này trong SeatService, ví dụ: "findAvailableSeatsForTrip")
        List<Seat> availableSeats = seatService.getAllSeats(); // Tạm thời lấy tất cả

        model.addAttribute("selectedTrip", tripOpt.get());
        model.addAttribute("availableSeats", availableSeats);
        model.addAttribute("currentUser", currentUser); // Gửi thông tin user đã login

        return "ticket/form"; // <-- Trả về Trang 3 (file ticket/form.html)
    }

    /**
     * HÀM MỚI (Trang 3 Customer): Xử lý đặt vé
     */
    @PostMapping
    public String createBooking(HttpSession session, // Lấy user từ session
                                @RequestParam("tripId") Long tripId,
                                @RequestParam("seatId") Long seatId,
                                @RequestParam("passengerName") String passengerName,
                                @RequestParam(value = "phone", required = false) String phone,
                                @RequestParam(value = "email", required = false) String email,
                                RedirectAttributes redirectAttributes) {

        User currentUser = (User) session.getAttribute("userLogin");
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            // Dùng ID của user đã login
            Booking booking = bookingService.createBooking(currentUser.getId(), tripId, seatId, passengerName, phone, email);
            redirectAttributes.addFlashAttribute("successMessage", "Đặt vé thành công. Mã đặt chỗ: " + booking.getBookingId());
            return "redirect:/bookings"; // Chuyển đến trang "Vé của tôi"
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/bookings/new?tripId=" + tripId; // Lỗi thì quay lại
        }
    }

    /**
     * HÀM CŨ (Trang "Vé của tôi" Customer): Hiển thị danh sách vé
     */
    @GetMapping
    public String listBookings(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("userLogin");
        if (currentUser == null) {
            return "redirect:/login";
        }

        // Lấy vé CỦA USER NÀY THÔI
        List<Booking> bookings = bookingService.findAllBookingsByUserId(currentUser.getId());
        model.addAttribute("bookings", bookings);
        return "ticket/list"; // Trỏ đến file list.html
    }

    /**
     * HÀM CŨ (Trang "Chi tiết vé" Customer):
     */
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
        return "ticket/detail"; // Trỏ đến file detail.html
    }
}