package com.example.trainticketoffice.config;

import com.example.trainticketoffice.common.*;
import com.example.trainticketoffice.model.*;
import com.example.trainticketoffice.repository.*;
import com.example.trainticketoffice.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;
    private final UserRepository userRepository;
    private final StationService stationService;
    private final RouteService routeService;
    private final TrainService trainService;
    private final SeatService seatService;
    private final TripService tripService;
    private final BookingService bookingService;
    private final TicketRepository ticketRepository;
    private final PaymentRepository paymentRepository;
    private final CarriageRepository carriageRepository;
    private final BookingRepository bookingRepository; // <-- ĐÃ THÊM

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        LocalDate today = LocalDate.now();

        // --- TẠO USER ---
        User staff = new User();
        staff.setEmail("staff@example.com");
        staff.setPassword("password123");
        staff.setFullName("Nguyễn Văn B");
        staff.setPhone("0911001100");
        staff.setCreateDate(today.minusMonths(6));
        staff.setRole(User.Role.STAFF);
        userService.addUser(staff);

        User customer = new User();
        customer.setEmail("customer@example.com");
        customer.setPassword("password123");
        customer.setFullName("Nguyễn Văn A");
        customer.setPhone("0909009009");
        customer.setCreateDate(today.minusMonths(5));
        customer.setRole(User.Role.CUSTOMER);
        userService.addUser(customer);

        // --- TẠO GA (STATION) ---
        Station haNoiStation = new Station();
        haNoiStation.setCode("HNO");
        haNoiStation.setName("Ga Hà Nội");
        haNoiStation.setCity("Hà Nội");
        haNoiStation.setProvince("Hà Nội");
        haNoiStation.setKmFromStart(new BigDecimal("0.00"));
        haNoiStation.setStatus(Station.Status.ACTIVE);
        haNoiStation = stationService.createStation(haNoiStation);

        Station saiGonStation = new Station();
        saiGonStation.setCode("HCM");
        saiGonStation.setName("Ga Sài Gòn");
        saiGonStation.setCity("Hồ Chí Minh");
        saiGonStation.setProvince("Hồ Chí Minh");
        saiGonStation.setKmFromStart(new BigDecimal("1726.00"));
        saiGonStation.setStatus(Station.Status.ACTIVE);
        saiGonStation = stationService.createStation(saiGonStation);

        // --- TẠO TUYẾN (ROUTE) ---
        Route northSouthRoute = new Route();
        northSouthRoute.setCode("HN-HCM");
        northSouthRoute.setStartStation(haNoiStation);
        northSouthRoute.setEndStation(saiGonStation);
        northSouthRoute.setTotalDistanceKm(new BigDecimal("1726.00"));
        northSouthRoute.setEstimatedDurationMinutes(1980);
        northSouthRoute.setStatus(Route.Status.ACTIVE);
        northSouthRoute = routeService.createRoute(northSouthRoute);

        // --- TẠO TÀU (TRAIN) ---
        Train se1Train = new Train();
        se1Train.setCode("SE1");
        se1Train.setName("Thống Nhất Express");
        se1Train.setTotalCarriages(15);
        se1Train.setSeatCapacity(600);
        se1Train.setStatus(TrainStatus.AVAILABLE);
        se1Train = trainService.saveTrain(se1Train);

        // --- TẠO TOA (CARRIAGE) ---
        Carriage toa1 = new Carriage();
        toa1.setTrain(se1Train);
        toa1.setName("Toa 1");
        toa1.setType("Ngồi mềm VIP");
        toa1.setPosition(1);
        toa1 = carriageRepository.save(toa1);

        Carriage toa2 = new Carriage();
        toa2.setTrain(se1Train);
        toa2.setName("Toa 2");
        toa2.setType("Ngồi mềm");
        toa2.setPosition(2);
        toa2 = carriageRepository.save(toa2);

        // --- TẠO GHẾ (SEAT) ---
        Seat vipSeat = new Seat();
        vipSeat.setCarriage(toa1);
        vipSeat.setSeatNumber("A1");
        vipSeat.setSeatType("VIP");
        vipSeat.setPricePerKm(new BigDecimal("1.50"));
        vipSeat.setStatus(SeatStatus.AVAILABLE);
        vipSeat.setIsActive(true);
        vipSeat = seatService.saveSeat(vipSeat);

        Seat normalSeat = new Seat();
        normalSeat.setCarriage(toa2);
        normalSeat.setSeatNumber("B12");
        normalSeat.setSeatType("normal");
        normalSeat.setPricePerKm(new BigDecimal("1.0"));
        normalSeat.setStatus(SeatStatus.AVAILABLE);
        normalSeat.setIsActive(true);
        normalSeat = seatService.saveSeat(normalSeat);

        // --- TẠO CHUYẾN (TRIP) ---
        LocalDateTime departureTime = today.plusDays(7).atTime(19, 0);
        LocalDateTime arrivalTime = today.plusDays(8).atTime(5, 30);

        Trip northSouthTrip = new Trip();
        northSouthTrip.setTrain(se1Train);
        northSouthTrip.setRoute(northSouthRoute);
        northSouthTrip.setDepartureStation(haNoiStation.getName());
        northSouthTrip.setArrivalStation(saiGonStation.getName());
        northSouthTrip.setPrice(1500000.0);
        northSouthTrip.setDepartureTime(departureTime);
        northSouthTrip.setArrivalTime(arrivalTime);
        northSouthTrip.setStatus(TripStatus.UPCOMING);
        northSouthTrip = tripService.saveTrip(northSouthTrip);

        // --- TẠO BOOKING (Đã thanh toán cho ghế A1) ---
        Booking booking = bookingService.createBooking(
                customer.getId(),
                northSouthTrip.getTripId(),
                vipSeat.getSeatId(),
                customer.getFullName(),
                customer.getPhone(),
                customer.getEmail()
        );

        Seat bookedSeat = booking.getSeat();

        // --- TẠO VÉ (TICKET) ---
        Ticket ticket = new Ticket();
        ticket.setCode("TICKET-SE1-0001");
        ticket.setBooking(booking);
        ticket.setTrip(booking.getTrip());
        ticket.setSeat(bookedSeat);
        ticket.setFromStation(haNoiStation);
        ticket.setToStation(saiGonStation);
        ticket.setPassengerName(customer.getFullName());
        ticket.setPassengerPhone(customer.getPhone());
        ticket.setPassengerIdCard("012345678901");
        ticket.setDistanceKm(northSouthRoute.getTotalDistanceKm());
        ticket.setTotalPrice(booking.getPrice()); // <-- SỬA: Lấy giá đúng
        ticket.setStatus(TicketStatus.ACTIVE);
        ticket.setBookedAt(LocalDateTime.now().minusDays(1));
        ticketRepository.save(ticket);

        // --- TẠO THANH TOÁN (PAYMENT) ---
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setUser(customer);
        payment.setAmount(booking.getPrice()); // <-- SỬA: Lấy giá đúng
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionRef("TXN123456");
        payment.setOrderInfo("Thanh toán vé tàu");
        payment.setBankCode("VCB");
        payment.setBankTranNo("202405010001");
        payment.setVnpTransactionNo("123456789");
        payment.setResponseCode("00");
        payment.setPayDate(LocalDateTime.now().minusDays(1).plusMinutes(15));
        payment.setSecureHash("samplehash");
        paymentRepository.save(payment);

        booking.setStatus(BookingStatus.PAID);
        bookingRepository.save(booking);
    }
}