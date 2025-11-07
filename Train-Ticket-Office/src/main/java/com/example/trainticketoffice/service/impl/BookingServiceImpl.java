package com.example.trainticketoffice.service.impl;

import com.example.trainticketoffice.common.BookingStatus;
import com.example.trainticketoffice.common.PaymentStatus; // THÊM
import com.example.trainticketoffice.common.SeatStatus;
import com.example.trainticketoffice.common.TicketStatus;
import com.example.trainticketoffice.model.*;
import com.example.trainticketoffice.repository.*;
import com.example.trainticketoffice.service.BookingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode; // THÊM
import java.time.Duration; // THÊM
import java.time.LocalDateTime;
import java.util.ArrayList; // THÊM
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
    private final OrderRepository orderRepository; // THÊM

    // THÊM: Hằng số cho giảm giá
    // (Giả sử: Trẻ em 6-9t. Người già > 60t. Em bé < 6t)
    private static final BigDecimal CHILD_DISCOUNT_PERCENT = new BigDecimal("0.5"); // Giảm 50%
    private static final BigDecimal SENIOR_DISCOUNT_PERCENT = new BigDecimal("0.15"); // Giảm 15%
    private static final BigDecimal INFANT_PRICE = BigDecimal.ZERO; // Miễn phí


    @Override
    @Transactional
    public Order createOrder(Integer userId,
                             Long tripId,
                             List<Long> seatIds,
                             String passengerName,
                             String passengerType,
                             String phone,
                             String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với mã " + userId));

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chuyến đi với mã " + tripId));

        // 1. Tạo Order trước
        Order order = new Order();
        order.setUser(user);
        order.setOrderTime(LocalDateTime.now());
        order.setBookings(new ArrayList<>());

        BigDecimal totalOrderPrice = BigDecimal.ZERO;

        for (Long seatId : seatIds) {
            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ghế với mã " + seatId));

            // 2. Kiểm tra ghế (logic giữ nguyên từ file cũ)
            if (seat.getStatus() == SeatStatus.BOOKED) {
                autoCancelExpiredBookingsForTrip(tripId);
                seat = seatRepository.findById(seatId).get(); // Lấy lại trạng thái ghế
                if(seat.getStatus() == SeatStatus.BOOKED) {
                    throw new IllegalStateException("Ghế " + seat.getSeatNumber() + " đã được đặt trước đó và vẫn còn thời hạn giữ.");
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

            // 3. Tính giá (Logic giá theo KM sẽ ở đây, hiện tại dùng giá ghế)
            BigDecimal originalPrice = seat.getPrice();
            BigDecimal finalPrice = originalPrice;

            // 4. Áp dụng giảm giá
            if ("CHILD".equals(passengerType)) {
                finalPrice = originalPrice.subtract(originalPrice.multiply(CHILD_DISCOUNT_PERCENT));
            } else if ("SENIOR".equals(passengerType)) {
                finalPrice = originalPrice.subtract(originalPrice.multiply(SENIOR_DISCOUNT_PERCENT));
            } else if ("INFANT".equals(passengerType)) {
                finalPrice = INFANT_PRICE;
            }

            // 5. Tạo Booking
            Booking booking = new Booking();
            booking.setUser(user);
            booking.setTrip(trip);
            booking.setSeat(seat);
            booking.setOrder(order); // <-- Liên kết Booking với Order
            booking.setPassengerName(passengerName);
            booking.setPassengerType(passengerType);
            booking.setPhone(phone);
            booking.setEmail(email);
            booking.setStatus(BookingStatus.BOOKED); // Chờ thanh toán
            booking.setBookingTime(order.getOrderTime());
            booking.setOriginalPrice(originalPrice);
            booking.setPrice(finalPrice.setScale(0, RoundingMode.HALF_UP));

            // 6. Cập nhật ghế
            seat.setStatus(SeatStatus.BOOKED);
            seatRepository.save(seat);

            bookingRepository.save(booking); // Lưu booking
            order.getBookings().add(booking);
            totalOrderPrice = totalOrderPrice.add(finalPrice);
        }

        // 7. Lưu Order
        order.setTotalPrice(totalOrderPrice.setScale(0, RoundingMode.HALF_UP));
        return orderRepository.save(order);
    }

    @Override
    public List<Booking> findAllBookings() { return bookingRepository.findAll(); }
    @Override
    public List<Booking> findAllBookingsByUserId(Integer userId) { return bookingRepository.findByUser_Id(userId); }
    @Override
    public Optional<Booking> findById(Long bookingId) { return bookingRepository.findById(bookingId); }

    @Transactional
    public void internalCancelBooking(Booking booking, boolean releaseSeat) {
        // SỬA: Cập nhật trạng thái, không xóa
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        if (releaseSeat) {
            Seat seat = booking.getSeat();
            if (seat != null) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seatRepository.save(seat);
            }
        }

        // Hủy các vé (Ticket) liên quan (nếu có)
        List<Ticket> tickets = ticketRepository.findByBooking(booking);
        for(Ticket ticket : tickets) {
            ticket.setStatus(TicketStatus.CANCELLED);
            ticketRepository.save(ticket);
        }
    }

    @Override
    @Transactional
    public int customerCancelBooking(Long bookingId, Integer userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy booking với ID: " + bookingId));

        if (!booking.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Bạn không có quyền hủy booking này.");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Vé đã ở trạng thái cuối (Đã hủy hoặc Hoàn thành).");
        }

        int refundPercent = 0;

        // THÊM: Logic hoàn tiền
        if (booking.getStatus() == BookingStatus.PAID) {
            LocalDateTime departureTime = booking.getTrip().getDepartureTime();
            long hoursBefore = Duration.between(LocalDateTime.now(), departureTime).toHours();

            // Chính sách: (Giảng viên yêu cầu)
            // >= 24h: hoàn 80% (mất 20%)
            // 4h - 24h: hoàn 50% (mất 50%)
            // < 4h: mất 100%
            if (hoursBefore >= 24) {
                refundPercent = 80;
            } else if (hoursBefore >= 4) {
                refundPercent = 50;
            } else {
                refundPercent = 0;
            }

            // (Trong dự án thực tế, đây là nơi gọi API hoàn tiền của VNPay)
            // Giả lập: Cập nhật DB và ghi log
            System.out.println("Hoàn tiền " + refundPercent + "% cho booking " + bookingId);

            // Cập nhật lại giá của Order (nếu hủy 1 vé trong đơn)
            Order order = booking.getOrder();
            if(order != null) {
                order.setTotalPrice(order.getTotalPrice().subtract(booking.getPrice()));
                orderRepository.save(order);
            }
        }

        // Hủy vé (BOOKED hoặc PAID)
        internalCancelBooking(booking, true);
        return refundPercent;
    }

    @Override
    @Transactional
    public void autoCancelExpiredBookingsForTrip(Long tripId) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(HOLD_DURATION_MINUTES);
        List<Booking> booked = bookingRepository.findAllByTrip_TripIdAndStatus(tripId, BookingStatus.BOOKED);

        int cancelCount = 0;
        for (Booking booking : booked) {
            if (booking.getBookingTime().isBefore(cutoffTime)) {
                // SỬA: Chỉ hủy nếu Order liên quan cũng PENDING
                // (booking.getOrder() == null là logic cũ, giữ lại để an toàn)
                if (booking.getOrder() == null || booking.getOrder().getStatus() == PaymentStatus.PENDING) {
                    internalCancelBooking(booking, true);
                    cancelCount++;
                }
            }
        }

        if(cancelCount > 0) {
            System.out.println("SCHEDULER: Đã tự động hủy " + cancelCount + " vé quá hạn cho chuyến " + tripId);
        }
    }
}