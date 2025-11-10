package com.example.trainticketoffice.service;

import com.example.trainticketoffice.model.Payment;

import java.util.Map;

public interface PaymentService {

    String createPaymentRedirectUrl(Long orderId,
                                    String bankCode,
                                    String orderInfo,
                                    String orderType,
                                    String locale,
                                    String clientIp);

    Payment handleVnpayReturn(Map<String, String> vnpayParams);

}