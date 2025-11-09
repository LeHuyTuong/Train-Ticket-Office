package com.example.trainticketoffice.service.impl;

import com.example.trainticketoffice.common.BookingStatus;
import com.example.trainticketoffice.common.PaymentStatus;
import com.example.trainticketoffice.common.SeatStatus; // THÊM LẠI
import com.example.trainticketoffice.model.*;
import com.example.trainticketoffice.repository.*;
import com.example.trainticketoffice.service.BookingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final int HOLD_DURATION_MINUTES = 15;
    private static final BigDecimal HOLIDAY_SURCHARGE_RATE = new BigDecimal("1.20");

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final SeatRepository seatRepository; // THÊM LẠI
    // private final CarriageRepository carriageRepository; // (Không cần)
    private final TicketRepository ticketRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    private boolean isHoliday(LocalDate date) {
        if (date.getMonth() == Month.JANUARY && date.getDayOfMonth() == 1) return true;
        if (date.getMonth() == Month.APRIL && date.getDayOfMonth() == 30) return true;
        if (date.getMonth() == Month.MAY && date.getDayOfMonth() == 1) return true;
        if (date.getMonth() == Month.SEPTEMBER && date.getDayOfMonth() == 2) return true;
        return false;
    }

    // (Xóa hàm createOrder 6 tham số)

    // ===== VIẾT LẠI HOÀN TOÀN HÀM NÀY (LOGIC "BẢN ĐỒ GHẾ") =====
    @Override
    @Transactional
    public Order createOrder(Integer userId,
                             Long tripId,
                             List<Long> seatIds, // SỬA
                             String passengerName,
                             String passengerType,
                             String phone,
                             String email) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng: " + userId));
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chuyến đi: " + tripId));

        Station startStation = trip.getRoute().getStartStation();
        Station endStation = trip.getRoute().getEndStation();
        if (startStation.getDistanceKm() == null || endStation.getDistanceKm() == null) {
            throw new IllegalStateException("Lỗi cấu hình: Ga chưa có thông tin KM.");
        }
        int distanceKm = Math.abs(endStation.getDistanceKm() - startStation.getDistanceKm());
        if (distanceKm == 0) distanceKm = 20;

        Order order = new Order();
        order.setUser(user);
        order.setOrderTime(LocalDateTime.now());
        order.setStatus(PaymentStatus.PENDING);
        order.setTotalPrice(BigDecimal.ZERO);
        Order savedOrder = orderRepository.save(order);

        BigDecimal calculatedTotalPrice = BigDecimal.ZERO;
        List<Booking> createdBookings = new ArrayList<>();
        boolean isTripOnHoliday = isHoliday(trip.getDepartureTime().toLocalDate());

        for (Long seatId : seatIds) {
            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ghế: " + seatId));

            Carriage carriage = seat.getCarriage();
            if (carriage.getSeatType() == null || carriage.getSeatType().getPricePerKm() == null) {
                throw new IllegalStateException("Lỗi cấu hình: Toa " + carriage.getName() + " chưa có Loại Ghế/Giá.");
            }
            SeatType seatType = carriage.getSeatType();
            BigDecimal pricePerKm = seatType.getPricePerKm();

            // 1. KIỂM TRA GHẾ (LOGIC SEAT)
            if (seat.getStatus() == SeatStatus.BOOKED) {
                autoCancelExpiredBookingsForTrip(tripId); // Dọn dẹp
                seat = seatRepository.findById(seatId)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ghế sau khi làm mới: " + seatId));
                if(seat.getStatus() == SeatStatus.BOOKED) {
                    throw new IllegalStateException("Ghế " + seat.getSeatNumber() + " đã được đặt.");
                }
            }
            // Dùng hàm repository cũ (đã khôi phục)
            if (bookingRepository.existsByTrip_TripIdAndSeat_SeatIdAndStatusIn(trip.getTripId(), seat.getSeatId(), List.of(BookingStatus.BOOKED, BookingStatus.PAID))) {
                throw new IllegalStateException("Ghế " + seat.getSeatNumber() + " đã được giữ chỗ cho chuyến đi này");
            }

            // 2. TẠO BOOKING
            Booking booking = new Booking();
            booking.setUser(user);
            booking.setTrip(trip);
            booking.setSeat(seat); // Gán Ghế
            // (Không gán Carriage hay seatNumber, vì đã có trong Seat)
            booking.setPassengerName(passengerName);
            booking.setPhone(phone);
            booking.setEmail(email);
            booking.setStatus(BookingStatus.BOOKED);
            booking.setBookingTime(LocalDateTime.now());

            // 3. TÍNH GIÁ ĐỘNG
            BigDecimal basePrice = pricePerKm.multiply(BigDecimal.valueOf(distanceKm));
            if (isTripOnHoliday) {
                basePrice = basePrice.multiply(HOLIDAY_SURCHARGE_RATE);
            }
            BigDecimal finalPrice = basePrice;
            if ("INFANT".equals(passengerType)) finalPrice = BigDecimal.ZERO;
            else if ("CHILD".equals(passengerType)) finalPrice = finalPrice.multiply(BigDecimal.valueOf(0.5));
            else if ("SENIOR".equals(passengerType)) finalPrice = finalPrice.multiply(BigDecimal.valueOf(0.75));

            booking.setPrice(finalPrice.setScale(0, RoundingMode.HALF_UP));

            booking.setOrder(savedOrder);
            createdBookings.add(booking);
            calculatedTotalPrice = calculatedTotalPrice.add(booking.getPrice());

            // 4. CẬP NHẬT TRẠNG THÁI GHẾ
            seat.setStatus(SeatStatus.BOOKED);
            seatRepository.save(seat);
        }

        bookingRepository.saveAll(createdBookings);
        savedOrder.setTotalPrice(calculatedTotalPrice);
        savedOrder.setBookings(createdBookings);
        return orderRepository.save(savedOrder);
    }
    // ===================================

    @Override
    public List<Booking> findAllBookings() { return bookingRepository.findAll(); }
    @Override
    public List<Booking> findAllBookingsByUserId(Integer userId) { return bookingRepository.findByUser_Id(userId); }
    @Override
    public Optional<Booking> findById(Long bookingId) { return bookingRepository.findById(bookingId); }

    // SỬA HÀM NÀY (ĐỂ GIẢI PHÓNG GHẾ)
    @Transactional
    public void internalCancelBooking(Booking booking) {
        // GIẢI PHÓNG GHẾ
        Seat seat = booking.getSeat();
        if (seat != null) {
            seat.setStatus(SeatStatus.AVAILABLE);
            seatRepository.save(seat);
        }

        List<Ticket> tickets = ticketRepository.findByBooking(booking);
        ticketRepository.deleteAll(tickets);
        Order order = booking.getOrder();
        if (order != null) {
            List<Payment> payments = paymentRepository.findByOrder(order);
            paymentRepository.deleteAll(payments);
        }
        bookingRepository.delete(booking);
    }

    @Override
    @Transactional
    public void customerCancelBooking(Long bookingId, Integer userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy booking: " + bookingId));
        if (!booking.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Bạn không có quyền hủy booking này.");
        }
        if (booking.getStatus() == BookingStatus.PAID || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Không thể hủy vé đã thanh toán/hoàn thành.");
        }
        Order order = booking.getOrder();
        if(order != null) {
            order.setTotalPrice(order.getTotalPrice().subtract(booking.getPrice()));
            orderRepository.save(order);
        }
        internalCancelBooking(booking);
    }

    // SỬA HÀM NÀY (ĐỂ DỌN DẸP GHẾ)
    @Override
    @Transactional
    public void autoCancelExpiredBookingsForTrip(Long tripId) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(HOLD_DURATION_MINUTES);
        List<Booking> booked = bookingRepository.findAllByTrip_TripIdAndStatus(tripId, BookingStatus.BOOKED);
        int cancelCount = 0;
        for (Booking booking : booked) {
            if (booking.getBookingTime().isBefore(cutoffTime)) {
                if (booking.getOrder() == null || booking.getOrder().getStatus() != PaymentStatus.SUCCESS) {
                    Order order = booking.getOrder();
                    if(order != null) {
                        order.setTotalPrice(order.getTotalPrice().subtract(booking.getPrice()));
                        orderRepository.save(order);
                    }
                    internalCancelBooking(booking); // (Hàm này đã bao gồm giải phóng ghế)
                    cancelCount++;
                }
            }
        }
        if(cancelCount > 0) {
            System.out.println("SCHEDULER: Đã tự động hủy " + cancelCount + " vé quá hạn cho chuyến " + tripId);
        }
    }
}