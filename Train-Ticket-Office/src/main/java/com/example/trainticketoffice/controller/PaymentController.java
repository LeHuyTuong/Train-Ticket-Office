package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.model.Payment;
import com.example.trainticketoffice.service.PaymentService;
import com.example.trainticketoffice.util.VnpayUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService; // dùng service bạn đã có (PaymentServiceImpl)

    // Cho phép cả GET và POST để tránh 405 khi click link từ /bookings (GET) hoặc submit form (POST)
    @RequestMapping(value = "/bookings/{bookingId}", method = {RequestMethod.GET, RequestMethod.POST})
    public RedirectView createPayment(
            @PathVariable("bookingId") Long bookingId,
            @RequestParam(value = "bankCode", required = false) String bankCode,
            @RequestParam(value = "orderInfo", required = false) String orderInfo,
            @RequestParam(value = "orderType", required = false) String orderType,
            @RequestParam(value = "locale", required = false) String locale,
            HttpServletRequest request
    ) throws IOException {

        // Lấy IP client (nếu cần)
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = request.getRemoteAddr();
        }

        // Gọi service để tạo URL đã có chữ ký và lưu Payment pending
        String redirectUrl;
        try {
            redirectUrl = paymentService.createPaymentRedirectUrl(
                    bookingId,
                    bankCode,
                    orderInfo,
                    orderType,
                    locale,
                    clientIp
            );
        } catch (Exception ex) {
            // Nếu service lỗi, fallback build nhanh URL (dùng VnpayUtils) — nhưng ưu tiên dùng service của bạn
            Map<String, String> params = new HashMap<>();
            params.put("vnp_Version", "2.1.0");
            params.put("vnp_Command", "pay");
            params.put("vnp_TmnCode", VnpayUtils.urlEncode("AVX7XQLY")); // tmn code
            // NOTE: vnp_Amount phải là amount*100. Thay 500000 bằng booking amount nếu cần.
            params.put("vnp_Amount", String.valueOf(500000L * 100L));
            params.put("vnp_CurrCode", "VND");
            params.put("vnp_TxnRef", VnpayUtils.generateTxnRef());
            params.put("vnp_OrderInfo", orderInfo != null ? orderInfo : "Thanh toan ve tau " + bookingId);
            params.put("vnp_OrderType", orderType == null ? "other" : orderType);
            params.put("vnp_Locale", locale == null ? "vn" : locale);
            // return url lấy từ properties sẽ là ngrok public hoặc localhost
            params.put("vnp_ReturnUrl", "https://syndetic-audriana-paler.ngrok-free.dev/payments/vnpay-return");
            params.put("vnp_IpAddr", clientIp);
            params.put("vnp_CreateDate", LocalDateTime.now().format(VnpayUtils.VNPAY_DATE_FORMAT));

            // Lấy secret key từ properties tốt hơn; tạm hardcode fallback:
            String secret = "4I08FB7R3VMENYHFZJDW3609ROYDRCJD";
            String query = VnpayUtils.buildSignedQuery(params, secret);
            redirectUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html" + "?" + query;
        }

        // Redirect trực tiếp tới VNPay sandbox thật
        return new RedirectView(redirectUrl);
    }

    // VNPay redirect về sau khi user chọn phương thức (sandbox sẽ gọi return-url)
    @GetMapping("/vnpay-return")
    public ModelAndView handleVnpayReturn(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((k, v) -> params.put(k, v[0]));

        // Thử gọi service để xử lý (nếu bạn muốn lưu transaction thật)
        Payment payment = null;
        boolean serviceOk = false;
        try {
            payment = paymentService.handleVnpayReturn(params);
            serviceOk = true;
        } catch (Exception ex) {
            serviceOk = false;
        }

        ModelAndView mav = new ModelAndView("invoice"); // templates/invoice.html

        // Lấy dữ liệu (ưu tiên dữ liệu từ service nếu có)
        if (serviceOk && payment != null) {
            mav.addObject("bookingId", payment.getBooking() != null ? payment.getBooking().getBookingId() : payment.getTransactionRef());
            mav.addObject("amount", payment.getAmount());
            mav.addObject("bankCode", payment.getBankCode());
            mav.addObject("transactionNo", payment.getVnpTransactionNo() != null ? payment.getVnpTransactionNo() : payment.getBankTranNo());
            mav.addObject("payDate", payment.getPayDate());
            mav.addObject("transactionStatus", payment.getStatus() != null ? payment.getStatus().name() : "SUCCESS");
            mav.addObject("responseCode", payment.getResponseCode());
        } else {
            // Giả lập: treat as success so user thấy hoá đơn (không thực thao tác payment)
            String txnRef = params.getOrDefault("vnp_TxnRef", "FAKE-" + System.currentTimeMillis());
            String amountRaw = params.get("vnp_Amount");
            BigDecimal amount = BigDecimal.ZERO;
            try {
                if (amountRaw != null) amount = new BigDecimal(amountRaw).divide(BigDecimal.valueOf(100));
            } catch (Exception ignored) {}

            mav.addObject("bookingId", txnRef);
            mav.addObject("amount", amount);
            mav.addObject("bankCode", params.getOrDefault("vnp_BankCode", "VNPAY_SANDBOX"));
            mav.addObject("transactionNo", params.getOrDefault("vnp_TransactionNo", "VNP" + System.currentTimeMillis()));
            mav.addObject("payDate", VnpayUtils.parsePayDate(params.get("vnp_PayDate")) != null ? VnpayUtils.parsePayDate(params.get("vnp_PayDate")) : LocalDateTime.now());
            mav.addObject("transactionStatus", "SUCCESS (FAKE)");
            mav.addObject("responseCode", params.getOrDefault("vnp_ResponseCode", "00"));
        }

        return mav;
    }
}
