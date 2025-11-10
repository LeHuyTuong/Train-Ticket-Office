package com.example.trainticketoffice.service.impl;


import com.example.trainticketoffice.common.BookingStatus;
import com.example.trainticketoffice.common.PaymentStatus;
import com.example.trainticketoffice.model.Booking;
import com.example.trainticketoffice.model.Order; // THÊM
import com.example.trainticketoffice.model.Payment;
import com.example.trainticketoffice.repository.BookingRepository;
import com.example.trainticketoffice.repository.OrderRepository; // THÊM
import com.example.trainticketoffice.repository.PaymentRepository;
import com.example.trainticketoffice.service.AdminWalletService;
import com.example.trainticketoffice.service.PaymentService;
import com.example.trainticketoffice.util.VnpayUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    private final OrderRepository orderRepository;
    private final AdminWalletService adminWalletService;
    @Value("${vnpay.tmn-code}")
    private String tmnCode;

    @Value("${vnpay.secret-key}")
    private String secretKey;

    @Value("${vnpay.pay-url}")
    private String payUrl;

    @Value("${vnpay.return-url}")
    private String returnUrl;

    // ===== SỬA HOÀN TOÀN LOGIC HÀM NÀY =====
    // Bây giờ nó nhận orderId thay vì bookingId
    @Override
    @Transactional
    public String createPaymentRedirectUrl(Long orderId, // ĐỔI TÊN BIẾN
                                           String bankCode,
                                           String orderInfo,
                                           String orderType,
                                           String locale,
                                           String clientIp) {
        // 1. Tìm Order thay vì Booking
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng với mã " + orderId));

        if (order.getStatus() == PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Đơn hàng này đã được thanh toán");
        }

        String resolvedIp = (clientIp == null || clientIp.isBlank()) ? "127.0.0.1" : clientIp;
        // 2. Lấy tổng tiền từ Order
        BigDecimal amount = order.getTotalPrice().setScale(0, RoundingMode.HALF_UP);
        long amountValue = amount.multiply(BigDecimal.valueOf(100)).longValueExact();

        String resolvedOrderInfo = Optional.ofNullable(orderInfo)
                .filter(info -> !info.isBlank())
                .orElse("Thanh toan don hang " + order.getOrderId());

        String txnRef = VnpayUtils.generateTxnRef();

        Payment payment = new Payment();
        // 3. Liên kết Payment với Order
        payment.setOrder(order);
        payment.setUser(order.getUser());
        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionRef(txnRef);
        payment.setOrderInfo(resolvedOrderInfo);

        if (bankCode != null && !bankCode.isBlank()) {
            payment.setBankCode(bankCode);
        }

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_Amount", String.valueOf(amountValue));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", resolvedOrderInfo);
        params.put("vnp_OrderType", Optional.ofNullable(orderType).filter(s -> !s.isBlank()).orElse("other"));
        params.put("vnp_Locale", Optional.ofNullable(locale).filter(s -> !s.isBlank()).orElse("vn"));
        params.put("vnp_ReturnUrl", returnUrl);
        params.put("vnp_IpAddr", resolvedIp);
        params.put("vnp_CreateDate", LocalDateTime.now().format(VnpayUtils.VNPAY_DATE_FORMAT));
        if (bankCode != null && !bankCode.isBlank()) {
            params.put("vnp_BankCode", bankCode);
        }

        String query = VnpayUtils.buildSignedQuery(params, secretKey);
        payment.setSecureHash(query.substring(query.indexOf("vnp_SecureHash=") + "vnp_SecureHash=".length()));
        paymentRepository.save(payment);

        return payUrl + "?" + query;
    }

    // ===== SỬA LOGIC HÀM NÀY =====
    @Override
    @Transactional
    public Payment handleVnpayReturn(Map<String, String> vnpayParams) {
        String txnRef = vnpayParams.get("vnp_TxnRef");
        if (txnRef == null || txnRef.isBlank()) {
            throw new IllegalArgumentException("Thiếu mã giao dịch (vnp_TxnRef)");
        }

        Payment payment = paymentRepository.findByTransactionRef(txnRef)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch tương ứng với mã: " + txnRef));

        if(payment.getStatus() == PaymentStatus.SUCCESS) {
            return payment;
        }

        String receivedHash = vnpayParams.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isBlank()) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setResponseCode("HASH_MISSING");
            paymentRepository.save(payment);
            return payment;
        }

        boolean valid = VnpayUtils.validateSignature(vnpayParams, receivedHash, secretKey);
        if (!valid) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setResponseCode("SIGNATURE_INVALID");
            paymentRepository.save(payment);
            return payment;
        }
        // Kết thúc kiểm tra chữ ký

        String responseCode = vnpayParams.get("vnp_ResponseCode");
        payment.setResponseCode(responseCode);
        payment.setBankCode(vnpayParams.get("vnp_BankCode"));
        payment.setBankTranNo(vnpayParams.get("vnp_BankTranNo"));
        payment.setVnpTransactionNo(vnpayParams.get("vnp_TransactionNo"));
        payment.setPayDate(VnpayUtils.parsePayDate(vnpayParams.get("vnp_PayDate")));

        // 4. Lấy Order từ Payment
        Order order = payment.getOrder();

        if ("00".equals(responseCode)) {
            payment.setStatus(PaymentStatus.SUCCESS);

            // 5. Cập nhật trạng thái Order
            order.setStatus(PaymentStatus.SUCCESS);
            orderRepository.save(order);

            // 6. Cập nhật trạng thái TẤT CẢ Bookings trong Order
            for(Booking booking : order.getBookings()) {
                booking.setStatus(BookingStatus.PAID);
                bookingRepository.save(booking);
            }
            try {
                adminWalletService.addToBalance(payment.getAmount());
            } catch (Exception e) {
                // Ghi log lỗi nếu không cộng được tiền, nhưng không làm hỏng giao dịch của khách
                System.err.println("LỖI NGHIÊM TRỌNG: Không thể cộng tiền vào ví Admin cho giao dịch " + txnRef);
                e.printStackTrace();
            }
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            order.setStatus(PaymentStatus.FAILED);
            orderRepository.save(order);
            // (Không giải phóng ghế, để người dùng tự hủy booking)
        }

        paymentRepository.save(payment);
        return payment;
    }
}