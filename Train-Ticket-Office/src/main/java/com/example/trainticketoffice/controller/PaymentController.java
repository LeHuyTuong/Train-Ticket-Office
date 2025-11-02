package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.common.PaymentStatus;
import com.example.trainticketoffice.model.Booking;
import com.example.trainticketoffice.model.Payment;
import com.example.trainticketoffice.service.BookingService;
import com.example.trainticketoffice.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final BookingService bookingService;


    @GetMapping("/bookings/{bookingId}")
    public String showPaymentPage(@PathVariable Long bookingId,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        Optional<Booking> booking = bookingService.findById(bookingId);
        if (booking.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy thông tin đặt vé");
            return "redirect:/bookings";
        }
        model.addAttribute("booking", booking.get());
        return "payment/checkout";
    }

    @PostMapping("/bookings/{bookingId}")
    public String startPayment(@PathVariable Long bookingId,
                               @RequestParam(value = "bankCode", required = false) String bankCode,
                               @RequestParam(value = "orderInfo", required = false) String orderInfo,
                               @RequestParam(value = "orderType", required = false) String orderType,
                               @RequestParam(value = "locale", required = false) String locale,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        try {
            String paymentUrl = paymentService.createPaymentRedirectUrl(
                    bookingId,
                    bankCode,
                    orderInfo,
                    orderType,
                    locale,
                    resolveClientIp(request)
            );
            return "redirect:" + paymentUrl;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/payments/bookings/" + bookingId;
        }
    }

    @GetMapping("/vnpay-return")
    public String handleVnpayReturn(HttpServletRequest request, Model model) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, value) -> params.put(key, value[0]));
        try {
            Payment payment = paymentService.handleVnpayReturn(params);
            model.addAttribute("payment", payment);
            model.addAttribute("booking", payment.getBooking());
            model.addAttribute("success", payment.getStatus() == PaymentStatus.SUCCESS);
            model.addAttribute("message", payment.getStatus() == PaymentStatus.SUCCESS
                    ? "Thanh toán thành công"
                    : "Thanh toán không thành công (" + payment.getResponseCode() + ")");
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
        }
        return "payment/result";
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String remote = request.getRemoteAddr();
        return remote == null || remote.isBlank() ? "127.0.0.1" : remote;
    }
}

