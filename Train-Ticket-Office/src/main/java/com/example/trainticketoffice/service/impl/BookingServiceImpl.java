package com.example.trainticketoffice.service.impl;

import com.example.trainticketoffice.common.BookingStatus;
import com.example.trainticketoffice.common.PaymentStatus;
import com.example.trainticketoffice.common.SeatStatus;
import com.example.trainticketoffice.model.*;
import com.example.trainticketoffice.repository.*;
import com.example.trainticketoffice.service.BookingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final int HOLD_DURATION_MINUTES = 15;

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    /**
     * Implement phương thức rút gọn (gọi từ Controller).
     * Nó gọi phương thức đầy đủ với passengerType = null.
     */
    @Override
    @Transactional
    public Order createOrder(Integer userId,
                             Long tripId,
                             List<Long> seatIds,
                             String passengerName,
                             String phone,
                             String email) {
        // Gọi hàm đầy đủ, gán passengerType là null (hoặc "ADULT" nếu bạn muốn)
        return this.createOrder(userId, tripId, seatIds, passengerName, null, phone, email);
    }

    /**
     * Implement phương thức đầy đủ (gọi từ DataInitializer và Controller).
     */
    @Override
    @Transactional
    public Order createOrder(Integer userId,
                             Long tripId,
                             List<Long> seatIds,
                             String passengerName,
                             String passengerType, // Biến này đã được nhận
                             String phone,
                             String email) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với mã " + userId));

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chuyến đi với mã " + tripId));

        // 1. TẠO VÀ LƯU ORDER TRƯỚC TIÊN
        Order order = new Order();
        order.setUser(user);
        order.setOrderTime(LocalDateTime.now());
        order.setStatus(PaymentStatus.PENDING);
        order.setTotalPrice(BigDecimal.ZERO);

        Order savedOrder = orderRepository.save(order);

        BigDecimal calculatedTotalPrice = BigDecimal.ZERO;
        List<Booking> createdBookings = new ArrayList<>();

        for (Long seatId : seatIds) {
            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ghế với mã " + seatId));

            // 2. KIỂM TRA GHẾ
            if (seat.getStatus() == SeatStatus.BOOKED) {
                autoCancelExpiredBookingsForTrip(tripId);
                seat = seatRepository.findById(seatId)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ghế sau khi làm mới: " + seatId));
                if(seat.getStatus() == SeatStatus.BOOKED) {
                    throw new IllegalStateException("Ghế " + seat.getSeatNumber() + " đã được đặt.");
                }
            }

            boolean hasConflict = bookingRepository.existsByTrip_TripIdAndSeat_SeatIdAndStatusIn(
                    trip.getTripId(),
                    seat.getSeatId(),
                    List.of(BookingStatus.BOOKED, BookingStatus.PAID)
            );

            if (hasConflict) {
                throw new IllegalStateException("Ghế " + seat.getSeatNumber() + " đã được giữ chỗ cho chuyến đi này");
            }

            // 3. TẠO BOOKING
            Booking booking = new Booking();
            booking.setUser(user);
            booking.setTrip(trip);
            booking.setSeat(seat);
            booking.setPassengerName(passengerName);
            // (Lưu ý: passengerType chưa được dùng, bạn cần thêm trường này vào Booking.java nếu muốn lưu)
            booking.setPhone(phone);
            booking.setEmail(email);
            booking.setStatus(BookingStatus.BOOKED);
            booking.setBookingTime(LocalDateTime.now());

            // TÍNH TOÁN GIẢM GIÁ (logic cơ bản)
            BigDecimal finalPrice = seat.getPrice();
            if ("CHILD".equals(passengerType)) {
                finalPrice = finalPrice.multiply(BigDecimal.valueOf(0.5)); // Giảm 50%
            } else if ("SENIOR".equals(passengerType)) {
                finalPrice = finalPrice.multiply(BigDecimal.valueOf(0.75)); // Giảm 25%
            }
            // Làm tròn tiền về số nguyên gần nhất
            booking.setPrice(finalPrice.setScale(0, BigDecimal.ROUND_HALF_UP));

            booking.setOrder(savedOrder); // Gán Order (đã có ID) vào Booking

            createdBookings.add(booking);
            calculatedTotalPrice = calculatedTotalPrice.add(booking.getPrice());

            seat.setStatus(SeatStatus.BOOKED);
            seatRepository.save(seat);
        }

        // 4. LƯU TẤT CẢ BOOKING
        bookingRepository.saveAll(createdBookings);

        // 5. CẬP NHẬT LẠI ORDER
        savedOrder.setTotalPrice(calculatedTotalPrice);
        savedOrder.setBookings(createdBookings);

        return orderRepository.save(savedOrder);
    }


    @Override
    public List<Booking> findAllBookings() { return bookingRepository.findAll(); }
    @Override
    public List<Booking> findAllBookingsByUserId(Integer userId) { return bookingRepository.findByUser_Id(userId); }
    @Override
    public Optional<Booking> findById(Long bookingId) { return bookingRepository.findById(bookingId); }

    @Transactional
    public void internalCancelBooking(Booking booking) {
        Seat seat = booking.getSeat();
        if (seat != null) {
            seat.setStatus(SeatStatus.AVAILABLE);
            seatRepository.save(seat);
        }

        List<Ticket> tickets = ticketRepository.findByBooking(booking);
        ticketRepository.deleteAll(tickets);

        // Sửa lỗi (findByBooking -> findByOrder)
        Order order = booking.getOrder();
        if (order != null) {
            // Dùng findByOrder như trong PaymentRepository
            List<Payment> payments = paymentRepository.findByOrder(order);
            paymentRepository.deleteAll(payments);
        }

        bookingRepository.delete(booking);
    }

    @Override
    @Transactional
    public void customerCancelBooking(Long bookingId, Integer userId) {

        // ===== ĐÂY LÀ DÒNG BỊ LỖI CÚ PHÁP (ĐÃ SỬA) =====
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy booking với ID: " + bookingId));
        // =============================================

        if (!booking.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Bạn không có quyền hủy booking này.");
        }

        if (booking.getStatus() == BookingStatus.PAID || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Không thể hủy vé đã thanh toán/hoàn thành. Vui lòng liên hệ quầy vé.");
        }

        // (Cần thêm logic: Hủy vé này cũng nên cập nhật lại tổng tiền của Order)

        internalCancelBooking(booking);
    }

    @Override
    @Transactional
    public void autoCancelExpiredBookingsForTrip(Long tripId) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(HOLD_DURATION_MINUTES);
        List<Booking> booked = bookingRepository.findAllByTrip_TripIdAndStatus(tripId, BookingStatus.BOOKED);

        int cancelCount = 0;
        for (Booking booking : booked) {
            if (booking.getBookingTime().isBefore(cutoffTime)) {
                // Chỉ hủy nếu booking này chưa thuộc 1 Order đã thanh toán
                if (booking.getOrder() == null || booking.getOrder().getStatus() != PaymentStatus.SUCCESS) {
                    internalCancelBooking(booking);
                    cancelCount++;
                }
            }
        }

        if(cancelCount > 0) {
            System.out.println("SCHEDULER: Đã tự động hủy " + cancelCount + " vé quá hạn cho chuyến " + tripId);
        }
    }
}