package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.common.PaymentStatus;
import com.example.trainticketoffice.model.Order;
import com.example.trainticketoffice.model.Payment;
import com.example.trainticketoffice.service.BookingService;
import com.example.trainticketoffice.service.PaymentService;
import com.example.trainticketoffice.repository.OrderRepository;
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
import java.util.ArrayList;
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


    @GetMapping("/orders/{orderId}")
    public String showPaymentPage(@PathVariable Long orderId,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {

        // 1. Tìm đơn hàng chính (đơn hàng được click)
        Optional<Order> primaryOrderOpt = orderRepository.findById(orderId);
        if (primaryOrderOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy thông tin đơn hàng");
            return "redirect:/bookings";
        }
        Order primaryOrder = primaryOrderOpt.get();

        // 2. Lấy Group ID để kiểm tra khứ hồi
        String groupId = primaryOrder.getRoundTripGroupId();
        List<Order> ordersToShow = new ArrayList<>();
        BigDecimal totalGroupPrice;

        if (groupId != null && !groupId.isBlank()) {
            // 3A. LÀ KHỨ HỒI: Lấy tất cả đơn hàng trong nhóm
            ordersToShow.addAll(orderRepository.findByRoundTripGroupId(groupId));
            // Sắp xếp (nếu cần)
            ordersToShow.sort((o1, o2) -> o1.getOrderTime().compareTo(o2.getOrderTime()));

            // Tính tổng tiền của cả nhóm
            totalGroupPrice = ordersToShow.stream()
                    .map(Order::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
            // 3B. LÀ MỘT CHIỀU: Chỉ thêm đơn hàng này
            ordersToShow.add(primaryOrder);
            totalGroupPrice = primaryOrder.getTotalPrice();
        }

        // 4. Gửi các biến mà View (checkout.html) cần
        model.addAttribute("orders", ordersToShow); // Danh sách cho th:each
        model.addAttribute("totalGroupPrice", totalGroupPrice); // Tổng tiền
        model.addAttribute("primaryOrder", primaryOrder); // Đơn hàng chính để lấy ID cho form

        return "payment/checkout";
    }

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


    @GetMapping("/vnpay-return")
    public String handleVnpayReturn(HttpServletRequest request, Model model,
                                    HttpSession session) { // Nhận session
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, value) -> params.put(key, value[0]));

        try {
            Payment payment = paymentService.handleVnpayReturn(params, session);

            boolean isSuccess = payment.getStatus() == PaymentStatus.SUCCESS;

            String transactionNo = payment.getVnpTransactionNo() != null
                    ? payment.getVnpTransactionNo()
                    : payment.getTransactionRef();

            model.addAttribute("transactionNo", transactionNo);
            model.addAttribute("orderId", payment.getOrder().getOrderId());
            model.addAttribute("bankCode", payment.getBankCode());

            // Sửa: Lấy tổng tiền thực tế đã thanh toán từ vnpay param (vì khứ hồi)
            BigDecimal paidAmount = new BigDecimal(params.get("vnp_Amount"))
                    .divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP);
            model.addAttribute("amount", paidAmount);

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
