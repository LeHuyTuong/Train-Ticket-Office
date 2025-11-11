package com.example.trainticketoffice.service.impl;


import com.example.trainticketoffice.common.BookingStatus;
import com.example.trainticketoffice.common.PaymentStatus;
import com.example.trainticketoffice.model.Booking;
import com.example.trainticketoffice.model.Order;
import com.example.trainticketoffice.model.Payment;
import com.example.trainticketoffice.repository.BookingRepository;
import com.example.trainticketoffice.repository.OrderRepository;
import com.example.trainticketoffice.repository.PaymentRepository;
import com.example.trainticketoffice.service.AdminWalletService;
import com.example.trainticketoffice.service.PaymentService;
import com.example.trainticketoffice.util.VnpayUtils;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    /**
     * REFACTOR THEO YÊU CẦU: Tính tổng tiền của Group
     */
    @Override
    @Transactional
    public String createPaymentRedirectUrl(Long orderId, // Đây là orderId "chính"
                                           String bankCode,
                                           String orderInfo,
                                           String orderType,
                                           String locale,
                                           String clientIp) {

        // 1. Tìm Order "chính" (đơn hàng cuối cùng được tạo)
        Order primaryOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng với mã " + orderId));

        if (primaryOrder.getStatus() == PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Đơn hàng này đã được thanh toán");
        }

        // 2. REFACTOR: Lấy Group ID từ Order
        String groupId = primaryOrder.getRoundTripGroupId();
        BigDecimal totalAmount; // Tổng tiền sẽ thanh toán
        BigDecimal totalAmountForPaymentRecord; // Tiền lưu vào record (chỉ là tiền của order chính)

        if (groupId != null && !groupId.isBlank()) {
            // 3.A. Nếu là khứ hồi, tính TỔNG tiền của cả nhóm
            List<Order> groupOrders = orderRepository.findByRoundTripGroupId(groupId);
            totalAmount = groupOrders.stream()
                    .map(Order::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            // Lưu tiền của order chính vào record payment
            totalAmountForPaymentRecord = primaryOrder.getTotalPrice();
        } else {
            // 3.B. Nếu là 1 chiều, lấy tiền của đơn hàng đó
            totalAmount = primaryOrder.getTotalPrice();
            totalAmountForPaymentRecord = primaryOrder.getTotalPrice();
        }

        // 4. Tạo thanh toán (Vẫn dựa trên đơn hàng "chính")

        // SỬA LỖI 1: Xóa khai báo 'resolvedIp' bị trùng
        String resolvedIp = (clientIp == null || clientIp.isBlank()) ? "127.0.0.1" : clientIp;

        // SỬA LỖI 2: Xóa khai báo 'amountValue' bị trùng
        // REFACTOR: Lấy tổng tiền (đã tính ở trên) và nhân 100
        long amountValue = totalAmount.setScale(0, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .longValueExact();

        String resolvedOrderInfo = Optional.ofNullable(orderInfo)
                .filter(info -> !info.isBlank())
                .orElse("Thanh toan don hang " + primaryOrder.getOrderId());

        String txnRef = VnpayUtils.generateTxnRef();

        //payment
        Payment payment = new Payment();
        payment.setOrder(primaryOrder); // Link payment với order "chính"
        payment.setUser(primaryOrder.getUser());
        payment.setAmount(totalAmountForPaymentRecord); // Lưu số tiền của order chính
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionRef(txnRef);
        payment.setOrderInfo(resolvedOrderInfo);

        if (bankCode != null && !bankCode.isBlank()) {
            payment.setBankCode(bankCode);
        }

        // 6. Build params cho VNPay
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_Amount", String.valueOf(amountValue)); // <-- REFACTOR: Dùng TỔNG tiền (đã * 100)
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

        // 7. Tạo chữ ký và lưu Payment
        String query = VnpayUtils.buildSignedQuery(params, secretKey);
        payment.setSecureHash(query.substring(query.indexOf("vnp_SecureHash=") + "vnp_SecureHash=".length()));
        paymentRepository.save(payment);

        return payUrl + "?" + query;
    }

    @Override
    @Transactional
    public Payment handleVnpayReturn(Map<String, String> vnpayParams, HttpSession session) {
        String txnRef = vnpayParams.get("vnp_TxnRef");
        if (txnRef == null || txnRef.isBlank()) {
            throw new IllegalArgumentException("Thiếu mã giao dịch (vnp_TxnRef)");
        }

        // 1. Tìm bản ghi Payment
        Payment payment = paymentRepository.findByTransactionRef(txnRef)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch tương ứng với mã: " + txnRef));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return payment; // Giao dịch đã được xử lý (tránh xử lý lặp)
        }

        // 2. Xác thực chữ ký
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
        String responseCode = vnpayParams.get("vnp_ResponseCode");
        payment.setResponseCode(responseCode);
        payment.setBankCode(vnpayParams.get("vnp_BankCode"));
        payment.setBankTranNo(vnpayParams.get("vnp_BankTranNo"));
        payment.setVnpTransactionNo(vnpayParams.get("vnp_TransactionNo"));
        payment.setPayDate(VnpayUtils.parsePayDate(vnpayParams.get("vnp_PayDate")));

        // SỬA LỖI 6: Dùng 'order' thay vì 'primaryOrder'
        Order order = payment.getOrder(); // Đây chính là "primaryOrder"

        // SỬA LỖI 3, 4, 5: Lấy 'groupId' từ 'order'
        String groupId = order.getRoundTripGroupId();

        // 5. REFACTOR: Chuẩn bị list các Order cần cập nhật
        List<Order> ordersToUpdate = new ArrayList<>();
        if (groupId != null && !groupId.isBlank()) {
            // Nếu là khứ hồi, lấy cả nhóm
            ordersToUpdate.addAll(orderRepository.findByRoundTripGroupId(groupId));
        } else {
            // Nếu là 1 chiều, chỉ lấy đơn hàng chính
            ordersToUpdate.add(order); // Sửa 'primaryOrder' thành 'order'
        }

        // 6. Xử lý kết quả thanh toán
        if ("00".equals(responseCode)) {
            // 6.A. THANH TOÁN THÀNH CÔNG
            payment.setStatus(PaymentStatus.SUCCESS);

            // SỬA LỖI LOGIC: Cập nhật TẤT CẢ order trong nhóm
            for (Order orderToUpdate : ordersToUpdate) {
                orderToUpdate.setStatus(PaymentStatus.SUCCESS);
                orderRepository.save(orderToUpdate);

                // Cập nhật TẤT CẢ booking trong từng order
                for (Booking booking : orderToUpdate.getBookings()) {
                    booking.setStatus(BookingStatus.PAID);
                    bookingRepository.save(booking);
                }
            }

            try {
                // Tính tổng tiền thực tế đã thanh toán (từ vnpay)
                BigDecimal paidAmount = new BigDecimal(vnpayParams.get("vnp_Amount"))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                adminWalletService.addToBalance(paidAmount);
            } catch (Exception e) {
                System.err.println("LỖI NGHIÊM TRỌNG: Không thể cộng tiền vào ví Admin cho giao dịch " + txnRef);
                e.printStackTrace();
            }
        } else {
            // 6.B. THANH TOÁN THẤT BẠI
            payment.setStatus(PaymentStatus.FAILED);

            // SỬA LỖI LOGIC: Cập nhật TẤT CẢ order trong nhóm
            for (Order orderToUpdate : ordersToUpdate) {
                orderToUpdate.setStatus(PaymentStatus.FAILED);
                orderRepository.save(orderToUpdate);
            }
        }

        paymentRepository.save(payment);
        return payment;
    }
}
