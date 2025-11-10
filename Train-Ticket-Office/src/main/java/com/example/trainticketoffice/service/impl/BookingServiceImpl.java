package com.example.trainticketoffice.service.impl;

import com.example.trainticketoffice.common.BookingStatus;
import com.example.trainticketoffice.common.PaymentStatus;
import com.example.trainticketoffice.common.SeatStatus;
import com.example.trainticketoffice.model.*; // (Dùng *)
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
import java.time.Period; // THÊM
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final int HOLD_DURATION_MINUTES = 15;
    private static final BigDecimal HOLIDAY_SURCHARGE_RATE = new BigDecimal("1.20");

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final SeatRepository seatRepository;
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

    // HÀM VALIDATE TUỔI
    private void validateAge(String passengerType, LocalDate dob) {
        if (dob == null) {
            throw new IllegalStateException("Vui lòng nhập Ngày sinh.");
        }

        int age = Period.between(dob, LocalDate.now()).getYears();

        switch (passengerType) {
            case "INFANT":
                if (age >= 6) throw new IllegalStateException("Tuổi của Trẻ Em (Miễn phí) phải dưới 6. Tuổi nhập vào là: " + age);
                break;
            case "CHILD":
                if (age < 6 || age > 10) throw new IllegalStateException("Tuổi của Trẻ Em (Giảm giá) phải từ 6-10. Tuổi nhập vào là: " + age);
                break;
            case "SENIOR":
                if (age < 60) throw new IllegalStateException("Tuổi của Người Cao Tuổi phải từ 60 trở lên. Tuổi nhập vào là: " + age);
                break;
            case "ADULT":
                if (age < 11 || age >= 60) throw new IllegalStateException("Tuổi của Người Lớn phải từ 11-59. Tuổi nhập vào là: " + age);
                break;
        }
    }

    @Override
    @Transactional
    public Order createOrder(BookingRequest bookingRequest, User user) {

        Trip trip = tripRepository.findById(bookingRequest.getTripId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chuyến đi: " + bookingRequest.getTripId()));

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

        //validation
        for (PassengerInfo passenger : bookingRequest.getPassengers()) {
            validateAge(passenger.getPassengerType(), passenger.getDob());
            Seat seat = seatRepository.findById(passenger.getSeatId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ghế: " + passenger.getSeatId()));
            Carriage carriage = seat.getCarriage();
            SeatType seatType = carriage.getSeatType();
            if (seatType == null || seatType.getPricePerKm() == null) {
                throw new IllegalStateException("Lỗi cấu hình: Toa " + carriage.getName() + " chưa có Loại Ghế/Giá.");
            }
            BigDecimal pricePerKm = seatType.getPricePerKm();

            if (seat.getStatus() == SeatStatus.BOOKED) {
                throw new IllegalStateException("Ghế " + seat.getSeatNumber() + " đã bị đặt mất. Vui lòng thử lại.");
            }
            if (bookingRepository.existsByTrip_TripIdAndSeat_SeatIdAndStatusIn(trip.getTripId(), seat.getSeatId(), List.of(BookingStatus.BOOKED, BookingStatus.PAID))) {
                throw new IllegalStateException("Ghế " + seat.getSeatNumber() + " đã bị giữ chỗ.");
            }

            Booking booking = new Booking();
            booking.setUser(user);
            booking.setTrip(trip);
            booking.setSeat(seat);

            booking.setPassengerName(passenger.getPassengerName());
            booking.setPhone(passenger.getPhone());
            booking.setEmail(passenger.getEmail());
            booking.setPassengerType(passenger.getPassengerType());
            booking.setPassengerIdCard(passenger.getPassengerIdCard());
            booking.setDob(passenger.getDob());
            booking.setStatus(BookingStatus.BOOKED);
            booking.setBookingTime(LocalDateTime.now());
            BigDecimal basePrice = pricePerKm.multiply(BigDecimal.valueOf(distanceKm));
            if (isTripOnHoliday) {
                basePrice = basePrice.multiply(HOLIDAY_SURCHARGE_RATE);
            }

            BigDecimal finalPrice = basePrice;
            String pType = passenger.getPassengerType();
            if ("INFANT".equals(pType)) finalPrice = BigDecimal.ZERO;
            else if ("CHILD".equals(pType)) finalPrice = finalPrice.multiply(BigDecimal.valueOf(0.5));
            else if ("SENIOR".equals(pType)) finalPrice = finalPrice.multiply(BigDecimal.valueOf(0.75));

            booking.setPrice(finalPrice.setScale(0, RoundingMode.HALF_UP));

            booking.setOrder(savedOrder);
            createdBookings.add(booking);
            calculatedTotalPrice = calculatedTotalPrice.add(booking.getPrice());

            seat.setStatus(SeatStatus.BOOKED);
            seatRepository.save(seat);
        }

        bookingRepository.saveAll(createdBookings);
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
                    Seat seat = booking.getSeat();
                    if (seat != null) {
                        seat.setStatus(SeatStatus.AVAILABLE);
                        seatRepository.save(seat);
                    }
                    internalCancelBooking(booking);
                    cancelCount++;
                }
            }
        }
        if(cancelCount > 0) {
            System.out.println("SCHEDULER: Đã tự động hủy " + cancelCount + " vé quá hạn (logic Bản đồ ghế).");
        }
    }
}