package com.example.trainticketoffice.service.impl;


import com.example.trainticketoffice.common.BookingStatus;
import com.example.trainticketoffice.common.PaymentStatus;
import com.example.trainticketoffice.model.Booking;
import com.example.trainticketoffice.model.Payment;
import com.example.trainticketoffice.repository.BookingRepository;
import com.example.trainticketoffice.repository.PaymentRepository;
import com.example.trainticketoffice.service.PaymentService;
import com.example.trainticketoffice.util.VnpayUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value; // THÊM DÒNG NÀY
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    @Value("${vnpay.tmn-code}")
    private String tmnCode;

    @Value("${vnpay.secret-key}")
    private String secretKey;

    @Value("${vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String payUrl;

    @Value("${vnpay.return-url}")
    private String returnUrl;

    public PaymentServiceImpl(PaymentRepository paymentRepository, BookingRepository bookingRepository) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    @Transactional
    public String createPaymentRedirectUrl(Long bookingId,
                                           String bankCode,
                                           String orderInfo,
                                           String orderType,
                                           String locale,
                                           String clientIp) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt vé với mã " + bookingId));

        if (booking.getStatus() == BookingStatus.PAID) {
            throw new IllegalStateException("Vé đã được thanh toán");
        }

        String resolvedIp = (clientIp == null || clientIp.isBlank()) ? "127.0.0.1" : clientIp;

        BigDecimal amount = BigDecimal.valueOf(booking.getTrip().getPrice()).setScale(0, RoundingMode.HALF_UP);
        long amountValue = amount.multiply(BigDecimal.valueOf(100)).longValueExact();

        String resolvedOrderInfo = Optional.ofNullable(orderInfo)
                .filter(info -> !info.isBlank())
                .orElse("Thanh toan dat ve tau " + booking.getBookingId());

        String txnRef = VnpayUtils.generateTxnRef();

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setUser(booking.getUser());
        payment.setAmount(amount.doubleValue());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionRef(txnRef);
        payment.setOrderInfo(resolvedOrderInfo);
        payment.setBankCode(bankCode);

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

    @Override
    @Transactional
    public Payment handleVnpayReturn(Map<String, String> vnpayParams) {
        String txnRef = vnpayParams.get("vnp_TxnRef");
        if (txnRef == null || txnRef.isBlank()) {
            throw new IllegalArgumentException("Thiếu mã giao dịch");
        }

        Payment payment = paymentRepository.findByTransactionRef(txnRef)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch tương ứng"));

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

        if ("00".equals(responseCode)) {
            payment.setStatus(PaymentStatus.SUCCESS);
            Booking booking = payment.getBooking();
            booking.setStatus(BookingStatus.PAID);
            bookingRepository.save(booking);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }

        paymentRepository.save(payment);
        return payment;
    }
}
