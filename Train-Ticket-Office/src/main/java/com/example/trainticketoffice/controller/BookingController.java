package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.common.BookingStatus; // <-- THÊM
import com.example.trainticketoffice.model.*; // <-- SỬA
import com.example.trainticketoffice.repository.BookingRepository; // <-- THÊM
import com.example.trainticketoffice.repository.TripRepository;
import com.example.trainticketoffice.service.BookingService;
import com.example.trainticketoffice.service.SeatService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList; // <-- THÊM
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors; // <-- THÊM

@Controller
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final TripRepository tripRepository;
    private final SeatService seatService;
    private final BookingRepository bookingRepository; // <-- THÊM

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

        Trip selectedTrip = tripOpt.get();
        Train train = selectedTrip.getTrain();

        // 1. Lấy tất cả Toa của tàu này
        List<Carriage> carriages = train.getCarriages();

        // 2. Lấy tất cả Ghế của tàu này
        List<Seat> allSeatsOnTrain = carriages.stream()
                .flatMap(carriage -> carriage.getSeats().stream())
                .collect(Collectors.toList());

        // 3. Lấy ID của các ghế đã được đặt cho chuyến đi NÀY
        List<Long> bookedSeatIds = bookingRepository.findAllByTrip_TripIdAndStatusIn(
                        tripId,
                        List.of(BookingStatus.BOOKED, BookingStatus.PAID)
                )
                .stream()
                .map(booking -> booking.getSeat().getSeatId())
                .collect(Collectors.toList());

        model.addAttribute("selectedTrip", selectedTrip);
        model.addAttribute("carriages", carriages); // Gửi danh sách Toa
        model.addAttribute("allSeats", allSeatsOnTrain); // Gửi TẤT CẢ ghế
        model.addAttribute("bookedSeatIds", bookedSeatIds); // Gửi ID ghế đã bị đặt
        model.addAttribute("currentUser", currentUser);

        return "ticket/form"; // Trả về trang chọn ghế
    }

    /**
     * HÀM MỚI (Trang 3 Customer): Xử lý đặt vé (chọn nhiều ghế)
     */
    @PostMapping
    public String createBooking(HttpSession session, // Lấy user từ session
                                @RequestParam("tripId") Long tripId,
                                @RequestParam("seatIds") List<Long> seatIds,
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
            List<Booking> createdBookings = new ArrayList<>();
            for(Long seatId : seatIds) {
                Booking booking = bookingService.createBooking(
                        currentUser.getId(),
                        tripId,
                        seatId,
                        passengerName,
                        phone,
                        email
                );
                createdBookings.add(booking);
            }

            redirectAttributes.addFlashAttribute("successMessage", "Đặt " + createdBookings.size() + " ghế thành công!");
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