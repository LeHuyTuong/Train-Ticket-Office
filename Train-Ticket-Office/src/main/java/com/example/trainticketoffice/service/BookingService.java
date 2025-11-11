package com.example.trainticketoffice.service;

import com.example.trainticketoffice.model.Booking;
import com.example.trainticketoffice.model.BookingRequest; // THÊM
import com.example.trainticketoffice.model.Order;
import com.example.trainticketoffice.model.User; // THÊM

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface BookingService {


    /**
     * SỬA LẠI: Thêm tham số roundTripGroupId
     * @param roundTripGroupId (Có thể là null (cho vé 1 chiều), hoặc là 1 UUID (cho vé khứ hồi))
     */
    Order createOrder(BookingRequest bookingRequest, User user, String roundTripGroupId);

    List<Booking> findAllBookings();
    List<Booking> findAllBookingsByUserId(Integer userId);
    Optional<Booking> findById(Long bookingId);
    void customerCancelBooking(Long bookingId, Integer userId);
    void autoCancelExpiredBookingsForTrip(Long tripId);
    /**
     * Lấy tất cả dữ liệu cần thiết để hiển thị trang chọn ghế (ticket/form.html)
     * (Logic từ BookingController.showCreateForm)
     */
    Map<String, Object> getBookingFormDetails(Long tripId, User currentUser);

    /**
     * Chuẩn bị DTO (BookingRequest) cho trang nhập thông tin hành khách
     * (Logic từ BookingController.showPassengerDetailsForm)
     */
    BookingRequest preparePassengerDetails(Long tripId, List<Long> seatIds, User currentUser);

    /**
     * Lấy dữ liệu cho trang xác nhận (confirm-payment.html)
     * (Logic từ BookingController.showConfirmPage)
     */
    Map<String, Object> getConfirmationDetails(Long primaryOrderId, User currentUser);

    // GHI CHÚ: createOrder đã được sửa lại signature
    // Cần xóa hàm cũ trong Impl nếu nó không khớp
    Order createOrder(BookingRequest bookingRequest, User user);
}
