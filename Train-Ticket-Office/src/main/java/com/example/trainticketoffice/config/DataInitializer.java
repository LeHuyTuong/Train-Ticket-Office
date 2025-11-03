package com.example.trainticketoffice.config;

import com.example.trainticketoffice.common.PaymentStatus;
import com.example.trainticketoffice.common.SeatStatus;
import com.example.trainticketoffice.common.TicketStatus;
import com.example.trainticketoffice.model.*;
import com.example.trainticketoffice.repository.PaymentRepository;
import com.example.trainticketoffice.repository.TicketRepository;
import com.example.trainticketoffice.repository.UserRepository;
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

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        // Lấy ngày hiện tại
        LocalDate today = LocalDate.now();

        User staff = new User();
        staff.setEmail("staff@example.com");
        staff.setPassword("password123");
        staff.setFullName("Nguyễn Văn B");
        staff.setPhone("0911001100");
        staff.setCreateDate(today.minusMonths(6)); // Cập nhật ngày
        staff.setRole(User.Role.STAFF);
        userService.addUser(staff);

        User customer = new User();
        customer.setEmail("customer@example.com");
        customer.setPassword("password123");
        customer.setFullName("Nguyễn Văn A");
        customer.setPhone("0909009009");
        customer.setCreateDate(today.minusMonths(5)); // Cập nhật ngày
        customer.setRole(User.Role.CUSTOMER);
        userService.addUser(customer);

        Station haNoiStation = new Station();
        haNoiStation.setCode("HNO");
        haNoiStation.setName("Ga Hà Nội");
        haNoiStation.setCity("Hà Nội");
        haNoiStation.setProvince("Hà Nội");
        haNoiStation.setKmFromStart(new BigDecimal("0.00"));
        haNoiStation = stationService.createStation(haNoiStation);

        Station saiGonStation = new Station();
        saiGonStation.setCode("HCM");
        saiGonStation.setName("Ga Sài Gòn");
        saiGonStation.setCity("Hồ Chí Minh");
        saiGonStation.setProvince("Hồ Chí Minh");
        saiGonStation.setKmFromStart(new BigDecimal("1726.00"));
        saiGonStation = stationService.createStation(saiGonStation);

        Route northSouthRoute = new Route();
        northSouthRoute.setCode("HN-HCM");
        northSouthRoute.setStartStation(haNoiStation);
        northSouthRoute.setEndStation(saiGonStation);
        northSouthRoute.setTotalDistanceKm(new BigDecimal("1726.00"));
        northSouthRoute.setEstimatedDurationMinutes(1980);
        northSouthRoute = routeService.createRoute(northSouthRoute);

        Train se1Train = new Train();
        se1Train.setCode("SE1");
        se1Train.setName("Thống Nhất Express");
        se1Train.setTotalCarriages(15);
        se1Train.setSeatCapacity(600);
        se1Train.setStatus("ACTIVE");
        se1Train = trainService.saveTrain(se1Train);

        Seat vipSeat = new Seat();
        vipSeat.setTrain(se1Train);
        vipSeat.setSeatNumber("A1");
        vipSeat.setSeatType("VIP");
        vipSeat.setPricePerKm(1.50);
        vipSeat.setStatus(SeatStatus.AVAILABLE); // Để AVAILABLE, booking sẽ chuyển
        vipSeat.setIsActive(true);
        vipSeat = seatService.saveSeat(vipSeat);

        // Tạo thêm ghế để test đặt vé mới
        Seat normalSeat = new Seat();
        normalSeat.setTrain(se1Train);
        normalSeat.setSeatNumber("B12");
        normalSeat.setSeatType("normal");
        normalSeat.setPricePerKm(1.0);
        normalSeat.setStatus(SeatStatus.AVAILABLE);
        normalSeat.setIsActive(true);
        normalSeat = seatService.saveSeat(normalSeat);


        // ===== THAY ĐỔI QUAN TRỌNG =====
        // Đặt chuyến đi vào 7 ngày tới (so với ngày hiện tại)
        LocalDate departureDate = today.plusDays(7);
        LocalDate arrivalDate = today.plusDays(8);

        Trip northSouthTrip = new Trip();
        northSouthTrip.setTrain(se1Train);
        northSouthTrip.setRoute(northSouthRoute);
        northSouthTrip.setDepartureStation(haNoiStation.getName());
        northSouthTrip.setArrivalStation(saiGonStation.getName());
        northSouthTrip.setDepartureTime(departureDate); // <-- Dùng ngày tương lai
        northSouthTrip.setArrivalTime(arrivalDate); // <-- Dùng ngày tương lai
        northSouthTrip.setPrice(1_500_000.0);
        northSouthTrip = tripService.saveTrip(northSouthTrip);

        // Tạo 1 booking cũ (đã thanh toán) cho ghế A1
        Booking booking = bookingService.createBooking(
                customer.getId(),
                northSouthTrip.getTripId(),
                vipSeat.getSeatId(), // Đặt ghế A1
                customer.getFullName(),
                customer.getPhone(),
                customer.getEmail()
        );

        Seat bookedSeat = booking.getSeat(); // Ghế A1 sẽ bị chuyển thành BOOKED

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
        ticket.setTotalPrice(BigDecimal.valueOf(northSouthTrip.getPrice()));
        ticket.setStatus(TicketStatus.ACTIVE);
        ticket.setBookedAt(LocalDateTime.now().minusDays(1)); // Đặt hôm qua
        ticketRepository.save(ticket);

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setUser(customer);
        payment.setAmount(northSouthTrip.getPrice());
        payment.setStatus(PaymentStatus.SUCCESS); // Đã thanh toán thành công
        payment.setTransactionRef("TXN123456");
        payment.setOrderInfo("Thanh toán vé tàu");
        payment.setBankCode("VCB");
        payment.setBankTranNo("202405010001");
        payment.setVnpTransactionNo("123456789");
        payment.setResponseCode("00");
        payment.setPayDate(LocalDateTime.now().minusDays(1).plusMinutes(15)); // Thanh toán hôm qua
        payment.setSecureHash("samplehash");
        paymentRepository.save(payment);
    }
}