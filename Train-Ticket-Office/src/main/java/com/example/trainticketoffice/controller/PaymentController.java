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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final BookingService bookingService;
    private final OrderRepository orderRepository;

    // (Hàm này đã OK - Giữ nguyên)
    @GetMapping("/orders/{orderId}")
    public String showPaymentPage(@PathVariable Long orderId,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {

        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy thông tin đơn hàng");
            return "redirect:/bookings";
        }

        Order primaryOrder = orderOpt.get();
        String groupId = primaryOrder.getRoundTripGroupId();
        BigDecimal totalGroupPrice = BigDecimal.ZERO;
        List<Order> ordersToShow; // Khai báo

        if (groupId != null && !groupId.isBlank()) {
            // --- XỬ LÝ KHỨ HỒI (Gộp đơn) ---
            ordersToShow = orderRepository.findByRoundTripGroupId(groupId); // Gán
            for (Order o : ordersToShow) {
                totalGroupPrice = totalGroupPrice.add(o.getTotalPrice());
            }
        } else {
            // --- XỬ LÝ 1 CHIỀU ---
            totalGroupPrice = primaryOrder.getTotalPrice();
            ordersToShow = List.of(primaryOrder); // Gán
        }

        model.addAttribute("primaryOrder", primaryOrder);
        model.addAttribute("orders", ordersToShow); // Gửi list
        model.addAttribute("totalGroupPrice", totalGroupPrice);

        return "payment/checkout";
    }

    // (Hàm này đã OK - Giữ nguyên)
    @PostMapping("/orders/{orderId}")
    public String startPayment(@PathVariable Long orderId,
                               @RequestParam(value = "bankCode", required = false) String bankCode,
                               @RequestParam(value = "orderInfo", required = false) String orderInfo,
                               @RequestParam(value = "orderType", required = false) String orderType,
                               @RequestParam(value = "locale", required = false) String locale,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        try {
            String paymentUrl = paymentService.createPaymentRedirectUrl(
                    orderId,
                    bankCode,
                    orderInfo,
                    orderType,
                    locale,
                    resolveClientIp(request)
            );
            return "redirect:" + paymentUrl;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/payments/orders/" + orderId;
        }
    }

    /**
     * SỬA LẠI (Truyền session vào service)
     */
    @GetMapping("/vnpay-return")
    public String handleVnpayReturn(HttpServletRequest request, Model model,
                                    HttpSession session) { // Nhận session
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, value) -> params.put(key, value[0]));

        try {
            // ===== SỬA DÒNG NÀY =====
            // Truyền session vào service để service tự dọn dẹp
            Payment payment = paymentService.handleVnpayReturn(params, session);
            // ========================

            boolean isSuccess = payment.getStatus() == PaymentStatus.SUCCESS;

            // (XÓA KHỐI IF DỌN DẸP SESSION Ở ĐÂY)

            String transactionNo = payment.getVnpTransactionNo() != null
                    ? payment.getVnpTransactionNo()
                    : payment.getTransactionRef();

            model.addAttribute("transactionNo", transactionNo);
            model.addAttribute("orderId", payment.getOrder().getOrderId());
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

    // (Hàm này đã OK - Giữ nguyên)
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String remote = request.getRemoteAddr();
        return remote == null || remote.isBlank() ? "127.0.0.1" : remote;
    }
}
