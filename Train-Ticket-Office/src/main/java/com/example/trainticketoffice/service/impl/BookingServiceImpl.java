package com.example.trainticketoffice.service.impl;

import com.example.trainticketoffice.common.BookingStatus;
import com.example.trainticketoffice.common.PaymentStatus;
import com.example.trainticketoffice.common.ResourceNotFoundException;
import com.example.trainticketoffice.common.SeatStatus;
import com.example.trainticketoffice.model.*; // (Dùng *)
import com.example.trainticketoffice.repository.*;
import com.example.trainticketoffice.service.BookingService;
import com.example.trainticketoffice.service.SeatService; // <-- THÊM
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Period; // THÊM
import java.util.*;
import java.util.stream.Collectors; // <-- THÊM

@Service
@RequiredArgsConstructor // Sử dụng @RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final int HOLD_DURATION_MINUTES = 15;
    private static final BigDecimal HOLIDAY_SURCHARGE_RATE = new BigDecimal("1.20");

    // Các repo/service đã inject qua @RequiredArgsConstructor
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final SeatService seatService; // <-- THÊM (từ BookingController)
    private final StationRepository stationRepository; // <-- THÊM (từ BookingController)
    private final SeatTypeRepository seatTypeRepository; // <-- THÊM (từ BookingController)


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

    // ===== HÀM LOGIC MỚI (CHUYỂN TỪ CONTROLLER) =====

    @Override
    @Transactional(rollbackOn = Exception.class) // Đảm bảo rollback nếu có lỗi
    public Map<String, Object> getBookingFormDetails(Long tripId, User currentUser) {
        Map<String, Object> modelData = new HashMap<>();

        Trip selectedTrip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyến đi"));

        // Hủy vé hết hạn
        autoCancelExpiredBookingsForTrip(tripId);

        Train train = selectedTrip.getTrain();
        List<Carriage> carriages = train.getCarriages();
        List<Seat> allSeatsOnTrain = seatService.getAllSeats().stream()
                .filter(s -> s.getCarriage().getTrain().getId().equals(train.getId()))
                .collect(Collectors.toList());

        Station startStation = selectedTrip.getRoute().getStartStation();
        Station endStation = selectedTrip.getRoute().getEndStation();

        if (startStation.getDistanceKm() == null || endStation.getDistanceKm() == null) {
            throw new IllegalStateException("Lỗi cấu hình: Ga chưa có thông tin KM.");
        }
        int distanceKm = Math.abs(endStation.getDistanceKm() - startStation.getDistanceKm());
        if (distanceKm == 0) distanceKm = 20;

        boolean isTripOnHoliday = isHoliday(selectedTrip.getDepartureTime().toLocalDate());
        BigDecimal currentSurchargeRate = isTripOnHoliday ? HOLIDAY_SURCHARGE_RATE : BigDecimal.ONE;

        Map<Long, BigDecimal> seatPrices = new HashMap<>();
        for (Seat seat : allSeatsOnTrain) {
            SeatType seatType = seat.getCarriage().getSeatType();
            if (seatType != null && seatType.getPricePerKm() != null) {
                BigDecimal basePrice = seatType.getPricePerKm().multiply(BigDecimal.valueOf(distanceKm));
                BigDecimal priceWithSurcharge = basePrice.multiply(currentSurchargeRate);
                priceWithSurcharge = priceWithSurcharge.setScale(0, RoundingMode.HALF_UP);
                seatPrices.put(seat.getSeatId(), priceWithSurcharge);
            } else {
                // Giá lỗi để dễ nhận biết
                seatPrices.put(seat.getSeatId(), BigDecimal.valueOf(999999));
            }
        }

        List<Booking> allBookingsForTrip = bookingRepository.findAllByTrip_TripIdAndStatusIn(
                tripId, List.of(BookingStatus.BOOKED, BookingStatus.PAID, BookingStatus.COMPLETED)
        );

        List<Long> paidSeatIds = allBookingsForTrip.stream()
                .filter(b -> b.getStatus() == BookingStatus.PAID || b.getStatus() == BookingStatus.COMPLETED)
                .map(booking -> booking.getSeat().getSeatId())
                .collect(Collectors.toList());

        List<Long> pendingSeatIds = allBookingsForTrip.stream()
                .filter(b -> b.getStatus() == BookingStatus.BOOKED)
                .map(booking -> booking.getSeat().getSeatId())
                .collect(Collectors.toList());

        modelData.put("selectedTrip", selectedTrip);
        modelData.put("carriages", carriages);
        modelData.put("allSeats", allSeatsOnTrain);
        modelData.put("seatPrices", seatPrices);
        modelData.put("paidSeatIds", paidSeatIds);
        modelData.put("pendingSeatIds", pendingSeatIds);
        modelData.put("currentUser", currentUser);
        modelData.put("isHoliday", isTripOnHoliday);
        modelData.put("surchargeRate", HOLIDAY_SURCHARGE_RATE);

        return modelData;
    }

    @Override
    public BookingRequest preparePassengerDetails(Long tripId, List<Long> seatIds, User currentUser) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyến đi."));

        Station startStation = trip.getRoute().getStartStation();
        Station endStation = trip.getRoute().getEndStation();
        int distanceKm = Math.abs(endStation.getDistanceKm() - startStation.getDistanceKm());
        if (distanceKm == 0) distanceKm = 20;

        boolean isTripOnHoliday = isHoliday(trip.getDepartureTime().toLocalDate());
        BigDecimal currentSurchargeRate = isTripOnHoliday ? HOLIDAY_SURCHARGE_RATE : BigDecimal.ONE;

        BookingRequest bookingRequest = new BookingRequest();
        bookingRequest.setTripId(tripId);
        // (Context sẽ được set ở Controller)

        for (Long seatId : seatIds) {
            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new ResourceNotFoundException("Ghế không hợp lệ: " + seatId));

            SeatType seatType = seat.getCarriage().getSeatType();
            BigDecimal basePrice = seatType.getPricePerKm().multiply(BigDecimal.valueOf(distanceKm));
            if (isTripOnHoliday) {
                basePrice = basePrice.multiply(currentSurchargeRate);
            }

            PassengerInfo passenger = new PassengerInfo();
            passenger.setSeatId(seat.getSeatId());
            passenger.setSeatNumber(seat.getSeatNumber() + " (Toa " + seat.getCarriage().getName() + ")");
            passenger.setSeatTypeName(seatType.getName());
            passenger.setBasePrice(basePrice.setScale(0, RoundingMode.HALF_UP));

            // Set thông tin người dùng hiện tại cho hành khách ĐẦU TIÊN
            if (bookingRequest.getPassengers().isEmpty()) {
                passenger.setPassengerName(currentUser.getFullName());
                passenger.setPhone(currentUser.getPhone());
                passenger.setEmail(currentUser.getEmail());
            }

            bookingRequest.getPassengers().add(passenger);
        }

        return bookingRequest;
    }

    @Override
    @Transactional
    public Order createOrder(BookingRequest bookingRequest, User user) {
        // Đây là hàm cũ, gọi hàm mới với groupId = null
        return createOrder(bookingRequest, user, null);
    }


    /**
     * HÀM TẠO ORDER ĐÃ REFACTOR (thêm roundTripGroupId)
     */
    @Override
    @Transactional
    public Order createOrder(BookingRequest bookingRequest, User user, String roundTripGroupId) {

        Trip trip = tripRepository.findById(bookingRequest.getTripId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chuyến đi: " + bookingRequest.getTripId()));

        // (Sử dụng stationRepository đã tiêm)
        Station startStation = stationRepository.findById(trip.getRoute().getStartStation().getId())
                .orElseThrow(() -> new IllegalStateException("Lỗi cấu hình: Ga đi không tồn tại"));
        Station endStation = stationRepository.findById(trip.getRoute().getEndStation().getId())
                .orElseThrow(() -> new IllegalStateException("Lỗi cấu hình: Ga đến không tồn tại"));


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
        order.setRoundTripGroupId(roundTripGroupId); // <-- GÁN GROUP ID
        Order savedOrder = orderRepository.save(order);

        BigDecimal calculatedTotalPrice = BigDecimal.ZERO;
        List<Booking> createdBookings = new ArrayList<>();
        boolean isTripOnHoliday = isHoliday(trip.getDepartureTime().toLocalDate());

        //validation
        for (PassengerInfo passenger : bookingRequest.getPassengers()) {
            // 1. Validate tuổi
            validateAge(passenger.getPassengerType(), passenger.getDob());

            // 2. Lấy thông tin ghế, toa, loại ghế
            Seat seat = seatRepository.findById(passenger.getSeatId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ghế: " + passenger.getSeatId()));
            Carriage carriage = seat.getCarriage();
            // (Sử dụng seatTypeRepository đã tiêm)
            SeatType seatType = seatTypeRepository.findById(carriage.getSeatType().getId())
                    .orElseThrow(() -> new IllegalStateException("Lỗi cấu hình: Loại ghế không tồn tại"));

            if (seatType.getPricePerKm() == null) {
                throw new IllegalStateException("Lỗi cấu hình: Toa " + carriage.getName() + " chưa có Loại Ghế/Giá.");
            }
            BigDecimal pricePerKm = seatType.getPricePerKm();

            // 3. Kiểm tra logic nghiệp vụ (ghế đã bị đặt?)
            if (seat.getStatus() == SeatStatus.BOOKED) {
                throw new IllegalStateException("Ghế " + seat.getSeatNumber() + " đã bị đặt mất. Vui lòng thử lại.");
            }
            if (bookingRepository.existsByTrip_TripIdAndSeat_SeatIdAndStatusIn(trip.getTripId(), seat.getSeatId(), List.of(BookingStatus.BOOKED, BookingStatus.PAID))) {
                throw new IllegalStateException("Ghế " + seat.getSeatNumber() + " đã bị giữ chỗ.");
            }

            // 4. Tạo Booking
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

            // 5. Tính giá
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

            // 6. Liên kết và cập nhật
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
    public Map<String, Object> getConfirmationDetails(Long primaryOrderId, User currentUser) {
        Map<String, Object> modelData = new HashMap<>();

        Order justCreatedOrder = orderRepository.findById(primaryOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        String orderGroupId = justCreatedOrder.getRoundTripGroupId();

        List<Order> ordersToShow = new ArrayList<>();
        List<Booking> bookingsToShow = new ArrayList<>();
        Map<Long, String> bookingLegLabels = new HashMap<>();
        BigDecimal totalGroupPrice = BigDecimal.ZERO;
        boolean isRoundTripOrder = false;

        if (orderGroupId != null && !orderGroupId.isBlank()) {
            ordersToShow = orderRepository.findByRoundTripGroupId(orderGroupId);
            if (ordersToShow.isEmpty()) {
                ordersToShow.add(justCreatedOrder); // Fallback
            }

            ordersToShow.sort(Comparator.comparing(Order::getOrderTime));

            for (int index = 0; index < ordersToShow.size(); index++) {
                Order order = ordersToShow.get(index);
                totalGroupPrice = totalGroupPrice.add(order.getTotalPrice());
                String legLabel = (index == 0) ? "Lượt Đi" : "Lượt Về";
                for (Booking booking : order.getBookings()) {
                    bookingsToShow.add(booking);
                    bookingLegLabels.put(booking.getBookingId(), legLabel);
                }
            }
            isRoundTripOrder = ordersToShow.size() > 1;

        } else {
            // Xử lý 1 chiều
            ordersToShow.add(justCreatedOrder);
            bookingsToShow.addAll(justCreatedOrder.getBookings());
            totalGroupPrice = justCreatedOrder.getTotalPrice();
            for (Booking booking : bookingsToShow) {
                bookingLegLabels.put(booking.getBookingId(), "Một chiều");
            }
            isRoundTripOrder = false;
        }

        modelData.put("primaryOrderId", primaryOrderId);
        modelData.put("orders", ordersToShow);
        modelData.put("bookings", bookingsToShow);
        modelData.put("totalPrice", totalGroupPrice);
        modelData.put("totalTickets", bookingsToShow.size());
        modelData.put("isRoundTrip", isRoundTripOrder);
        modelData.put("bookingLegLabels", bookingLegLabels);

        return modelData;
    }


    // ===== CÁC HÀM GỐC (GIỮ NGUYÊN) =====

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
