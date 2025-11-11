package com.example.trainticketoffice.service;

import com.example.trainticketoffice.model.Payment;
import jakarta.servlet.http.HttpSession; // <-- THÊM IMPORT

import java.util.Map;

public interface PaymentService {

    String createPaymentRedirectUrl(Long orderId,
                                    String bankCode,
                                    String orderInfo,
                                    String orderType,
                                    String locale,
                                    String clientIp);

    // ===== SỬA HÀM NÀY (THÊM SESSION) =====
    Payment handleVnpayReturn(Map<String, String> vnpayParams, HttpSession session);
    // =====================================
}
