package com.example.trainticketoffice.service;

import com.example.trainticketoffice.model.Booking;
import com.example.trainticketoffice.model.RefundRequest;
import com.example.trainticketoffice.model.User;

import java.util.List;

public interface RefundService {

    /**
     * Khách hàng tạo yêu cầu hoàn vé.
     */
    RefundRequest createRefundRequest(Long bookingId, User user, String bankName, String accountNumber, String accountHolder);

    /**
     * Admin lấy danh sách vé chờ hoàn.
     */
    List<RefundRequest> getPendingRefunds();

    /**
     * Admin chấp thuận hoàn vé.
     */
    void approveRefund(Long refundRequestId, User adminUser);

    /**
     * Admin từ chối hoàn vé.
     */
    void rejectRefund(Long refundRequestId, User adminUser);
}