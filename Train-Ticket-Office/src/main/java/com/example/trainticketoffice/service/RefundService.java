package com.example.trainticketoffice.service;

import com.example.trainticketoffice.model.Booking;
import com.example.trainticketoffice.model.RefundRequest;
import com.example.trainticketoffice.model.User;

import java.util.List;

public interface RefundService {

    RefundRequest createRefundRequest(Long bookingId, User user, String bankName, String accountNumber, String accountHolder);
    List<RefundRequest> getPendingRefunds();
    void approveRefund(Long refundRequestId, User adminUser);
    void rejectRefund(Long refundRequestId, User adminUser);
}