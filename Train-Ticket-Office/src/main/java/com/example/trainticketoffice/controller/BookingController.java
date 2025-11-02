package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.model.Booking;
import com.example.trainticketoffice.model.Seat;
import com.example.trainticketoffice.model.Trip;
import com.example.trainticketoffice.model.User;
import com.example.trainticketoffice.repository.SeatRepository;
import com.example.trainticketoffice.repository.TripRepository;
import com.example.trainticketoffice.repository.UserRepository;
import com.example.trainticketoffice.service.BookingService;
import com.example.trainticketoffice.service.SeatService;
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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final SeatRepository seatRepository;
    private final SeatService seatService;

    @GetMapping
    public String listBookings(Model model) {
        List<Booking> bookings = bookingService.findAllBookings();
        model.addAttribute("bookings", bookings);
        return "booking/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        prepareReferenceData(model);
        return "booking/form";
    }

    @PostMapping
    public String createBooking(@RequestParam("userId") Integer userId,
                                @RequestParam("tripId") Long tripId,
                                @RequestParam("seatId") Long seatId,
                                @RequestParam("passengerName") String passengerName,
                                @RequestParam(value = "phone", required = false) String phone,
                                @RequestParam(value = "email", required = false) String email,
                                RedirectAttributes redirectAttributes) {
        try {
            Booking booking = bookingService.createBooking(userId, tripId, seatId, passengerName, phone, email);
            redirectAttributes.addFlashAttribute("successMessage", "Đặt vé thành công. Mã đặt chỗ: " + booking.getBookingId());
            return "redirect:/bookings";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/bookings/new";
        }
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
        return "booking/detail";
    }

    @GetMapping("/user/{userId}")
    public String viewBookingsByUser(@PathVariable Integer userId,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        List<Booking> bookings = bookingService.findAllBookingsByUserId(userId);
        if (bookings.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Người dùng chưa có vé nào");
            return "redirect:/bookings";
        }
        model.addAttribute("bookings", bookings);
        model.addAttribute("userId", userId);
        return "booking/list";
    }

    private void prepareReferenceData(Model model) {
        List<User> users = userRepository.findAll();
        List<Trip> trips = tripRepository.findAll();
        List<Seat> seats = seatService.getAvailableSeats();


        Map<Long, String> tripDescriptions = trips.stream()
                .collect(Collectors.toMap(Trip::getTripId,
                        trip -> String.format("%s → %s (%s)",
                                trip.getDepartureStation(),
                                trip.getArrivalStation(),
                                trip.getDepartureTime())));

        Map<Long, String> seatDescriptions = seats.stream()
                .collect(Collectors.toMap(Seat::getSeatId,
                        seat -> String.format("%s - %s (%s)",
                                seat.getTrain().getCode(),
                                seat.getSeatNumber(),
                                seat.getSeatType())));

        model.addAttribute("users", users);
        model.addAttribute("trips", trips);
        model.addAttribute("seats", seats);
        model.addAttribute("tripDescriptions", tripDescriptions);
    }
}
