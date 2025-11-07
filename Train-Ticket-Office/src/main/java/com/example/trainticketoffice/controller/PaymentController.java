package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.common.PaymentStatus;
import com.example.trainticketoffice.model.Booking;
import com.example.trainticketoffice.model.Order; // THÊM
import com.example.trainticketoffice.model.Payment;
import com.example.trainticketoffice.service.BookingService;
// import com.example.trainticketoffice.service.OrderService; // (Nếu có)
import com.example.trainticketoffice.service.PaymentService;
import com.example.trainticketoffice.repository.OrderRepository; // THÊM
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
    private final OrderRepository orderRepository; // THÊM

    // ===== SỬA URL VÀ LOGIC HÀM NÀY =====
    // URL mới: /payments/orders/{orderId} (SỐ NHIỀU)
    @GetMapping("/orders/{orderId}")
    public String showPaymentPage(@PathVariable Long orderId,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        // Tìm Order thay vì Booking
        Optional<Order> order = orderRepository.findById(orderId);
        if (order.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy thông tin đơn hàng");
            return "redirect:/bookings";
        }

        model.addAttribute("order", order.get());
        // Trả về trang "checkout" MỚI (xem file số 4)
        return "payment/checkout";
    }

    // ===== SỬA URL VÀ LOGIC HÀM NÀY =====
    // URL mới: /payments/orders/{orderId} (SỐ NHIỀU)
    @PostMapping("/orders/{orderId}")
    public String startPayment(@PathVariable Long orderId,
                               @RequestParam(value = "bankCode", required = false) String bankCode,
                               @RequestParam(value = "orderInfo", required = false) String orderInfo,
                               @RequestParam(value = "orderType", required = false) String orderType,
                               @RequestParam(value = "locale", required = false) String locale,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        try {
            // Truyền orderId thay vì bookingId
            String paymentUrl = paymentService.createPaymentRedirectUrl(
                    orderId,
                    bankCode,
                    orderInfo,
                    orderType,
                    locale,
                    resolveClientIp(request)
            );
            return "redirect:" + paymentUrl; // Chuyển hướng đến trang VNPay
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            // Quay lại trang checkout (số nhiều)
            return "redirect:/payments/orders/" + orderId;
        }
    }

    @GetMapping("/vnpay-return")
    public String handleVnpayReturn(HttpServletRequest request, Model model) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, value) -> params.put(key, value[0]));

        try {
            Payment payment = paymentService.handleVnpayReturn(params);
            boolean isSuccess = payment.getStatus() == PaymentStatus.SUCCESS;

            String transactionNo = payment.getVnpTransactionNo() != null
                    ? payment.getVnpTransactionNo()
                    : payment.getTransactionRef();

            model.addAttribute("transactionNo", transactionNo);
            model.addAttribute("orderId", payment.getOrder().getOrderId()); // Sửa: Lấy OrderId
            model.addAttribute("bankCode", payment.getBankCode());
            model.addAttribute("amount", payment.getAmount());
            model.addAttribute("payDate", payment.getPayDate());
            model.addAttribute("transactionStatus", isSuccess ? "Thành công" : "Thất bại");

            return "payment/invoice";

        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "payment/result";
        }
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