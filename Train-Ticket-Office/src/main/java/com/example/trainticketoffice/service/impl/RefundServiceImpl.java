package com.example.trainticketoffice.service.impl;

import com.example.trainticketoffice.common.BookingStatus;
import com.example.trainticketoffice.common.RefundStatus;
import com.example.trainticketoffice.common.SeatStatus;
import com.example.trainticketoffice.common.TripStatus;
import com.example.trainticketoffice.model.Booking;
import com.example.trainticketoffice.model.RefundRequest;
import com.example.trainticketoffice.model.Seat;
import com.example.trainticketoffice.model.User;
import com.example.trainticketoffice.repository.BookingRepository;
import com.example.trainticketoffice.repository.RefundRequestRepository;
import com.example.trainticketoffice.repository.SeatRepository;
import com.example.trainticketoffice.service.AdminWalletService;
import com.example.trainticketoffice.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final BookingRepository bookingRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final SeatRepository seatRepository;
    private final AdminWalletService adminWalletService;


    @Override
    @Transactional
    public RefundRequest createRefundRequest(Long bookingId, User user, String bankName, String accountNumber, String accountHolder) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vé."));


        if (!booking.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("Bạn không có quyền hoàn vé này.");
        }


        if (booking.getStatus() != BookingStatus.PAID) {
            throw new IllegalStateException("Chỉ có thể hoàn vé đã thanh toán.");
        }


        if (booking.getTrip().getStatus() != TripStatus.UPCOMING) {
            throw new IllegalStateException("Không thể hoàn vé khi chuyến tàu đã chạy, bị hủy, hoặc đã hoàn thành.");
        }


        if (refundRequestRepository.existsByBooking_BookingId(bookingId)) {
            throw new IllegalStateException("Bạn đã gửi yêu cầu hoàn vé cho vé này rồi.");
        }


        RefundRequest refund = new RefundRequest();
        refund.setBooking(booking);
        refund.setBankName(bankName);
        refund.setBankAccountNumber(accountNumber);
        refund.setAccountHolderName(accountHolder);
        refund.setStatus(RefundStatus.PENDING);
        refund.setRequestedAt(LocalDateTime.now());

        booking.setStatus(BookingStatus.PENDING_REFUND);
        bookingRepository.save(booking);

        return refundRequestRepository.save(refund);
    }

    //Admin lấy danh sách chờ duyệt
    @Override
    public List<RefundRequest> getPendingRefunds() {

        return refundRequestRepository.findByStatusWithDetails(RefundStatus.PENDING); // <-- Dòng MỚI
    }

    //Admin duyệt
    @Override
    @Transactional
    public void approveRefund(Long refundRequestId, User adminUser) {
        RefundRequest refund = refundRequestRepository.findById(refundRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu hoàn vé."));

        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new IllegalStateException("Yêu cầu này đã được xử lý.");
        }


        refund.setStatus(RefundStatus.APPROVED);
        refund.setProcessedAt(LocalDateTime.now());
        refundRequestRepository.save(refund);


        Booking booking = refund.getBooking();
        booking.setStatus(BookingStatus.REFUNDED);
        bookingRepository.save(booking);
        try {
            adminWalletService.subtractFromBalance(booking.getPrice());
        } catch (Exception e) {
            System.err.println("LỖI NGHIÊM TRỌNG: Không thể trừ tiền ví Admin cho refund " + refundRequestId);
            e.printStackTrace();

        }
        refund.setStatus(RefundStatus.APPROVED);
        refund.setProcessedAt(LocalDateTime.now());
        refundRequestRepository.save(refund);
        Seat seat = booking.getSeat();
        if (seat != null) {
            seat.setStatus(SeatStatus.AVAILABLE);
            seatRepository.save(seat);
        }
    }

    //Admin từ chối
    @Override
    @Transactional
    public void rejectRefund(Long refundRequestId, User adminUser) {
        RefundRequest refund = refundRequestRepository.findById(refundRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu hoàn vé."));

        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new IllegalStateException("Yêu cầu này đã được xử lý.");
        }

        // 1. Cập nhật Yêu cầu
        refund.setStatus(RefundStatus.REJECTED);
        refund.setProcessedAt(LocalDateTime.now());
        refundRequestRepository.save(refund);

        // 2. Trả vé về trạng thái "Đã thanh toán"
        Booking booking = refund.getBooking();
        booking.setStatus(BookingStatus.PAID);
        bookingRepository.save(booking);
    }
}