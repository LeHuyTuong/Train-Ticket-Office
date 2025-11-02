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

        User staff = new User();
        staff.setEmail("staff@example.com");
        staff.setPassword("password123");
        staff.setFullName("Nguyễn Văn B");
        staff.setPhone("0911001100");
        staff.setCreateDate(LocalDate.of(2023, 1, 15));
        staff.setRole(User.Role.STAFF);
        userService.addUser(staff);

        User customer = new User();
        customer.setEmail("customer@example.com");
        customer.setPassword("password123");
        customer.setFullName("Nguyễn Văn A");
        customer.setPhone("0909009009");
        customer.setCreateDate(LocalDate.of(2023, 2, 20));
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
        vipSeat.setStatus(SeatStatus.AVAILABLE);
        vipSeat.setIsActive(true);
        vipSeat = seatService.saveSeat(vipSeat);

        Trip northSouthTrip = new Trip();
        northSouthTrip.setTrain(se1Train);
        northSouthTrip.setRoute(northSouthRoute);
        northSouthTrip.setDepartureStation(haNoiStation.getName());
        northSouthTrip.setArrivalStation(saiGonStation.getName());
        northSouthTrip.setDepartureTime(LocalDate.of(2024, 5, 1));
        northSouthTrip.setArrivalTime(LocalDate.of(2024, 5, 2));
        northSouthTrip.setPrice(1_500_000.0);
        northSouthTrip = tripService.saveTrip(northSouthTrip);

        Booking booking = bookingService.createBooking(
                customer.getId(),
                northSouthTrip.getTripId(),
                vipSeat.getSeatId(),
                customer.getFullName(),
                customer.getPhone(),
                customer.getEmail()
        );

        Seat bookedSeat = booking.getSeat();

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
        ticket.setBookedAt(LocalDateTime.of(2024, 4, 10, 9, 15));
        ticketRepository.save(ticket);

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setUser(customer);
        payment.setAmount(northSouthTrip.getPrice());
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionRef("TXN123456");
        payment.setOrderInfo("Thanh toán vé tàu");
        payment.setBankCode("VCB");
        payment.setBankTranNo("202405010001");
        payment.setVnpTransactionNo("123456789");
        payment.setResponseCode("00");
        payment.setPayDate(LocalDateTime.of(2024, 4, 10, 9, 30));
        payment.setSecureHash("samplehash");
        paymentRepository.save(payment);
    }
}
