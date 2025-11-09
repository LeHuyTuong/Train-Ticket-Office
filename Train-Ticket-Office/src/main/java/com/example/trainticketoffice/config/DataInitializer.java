package com.example.trainticketoffice.config;

import com.example.trainticketoffice.common.*;
import com.example.trainticketoffice.model.*;
import com.example.trainticketoffice.repository.*;
import com.example.trainticketoffice.service.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
// Xóa: import java.util.Map;

/**
 * DataInitializer PHIÊN BẢN KHÔI PHỤC (Logic Bản đồ ghế / Seat Map)
 * Đã cập nhật (Giai đoạn 8.3) để dùng BookingRequest
 * Đã GIẢM TẢI (xóa bulk data) để khởi động nhanh
 */
@Component
public class DataInitializer implements CommandLineRunner {

    // (Tất cả các Service và Repo cần thiết)
    private final UserService userService;
    private final UserRepository userRepository;
    private final StationService stationService;
    private final StationRepository stationRepository;
    private final RouteService routeService;
    private final RouteRepository routeRepository;
    private final TrainService trainService;
    private final SeatService seatService; // <-- (Logic Bản đồ ghế)
    private final TripService tripService;
    private final BookingService bookingService;
    private final TicketRepository ticketRepository;
    private final PaymentRepository paymentRepository;
    private final CarriageRepository carriageRepository;
    private final BookingRepository bookingRepository;
    private final OrderRepository orderRepository;
    private final SeatTypeRepository seatTypeRepository; // <-- (Logic Giá/KM)

    // Các biến tạm
    private User customer;
    private Station stationHaNoi, stationVinh, stationHue, stationDaNang, stationNhaTrang, stationSaiGon;
    private Route routeHnSg, routeSgHn, routeHnVn, routeHnDng, routeDngSg, routeSgNt;
    private Train trainSE1, trainSE2, trainSE3, trainSE4, trainSE5, trainSE6;
    private Seat se1_vip_seat_A1, se1_normal_seat_B1, se3_vip_seat_A1;
    private Trip tripSE1, tripSE3;
    private SeatType seatTypeVip, seatTypeNormal;


    // ===== CONSTRUCTOR ĐÃ GỘP (Thêm TẤT CẢ) =====
    public DataInitializer(UserService userService, UserRepository userRepository, StationService stationService,
                           StationRepository stationRepository, RouteService routeService, TrainService trainService,
                           SeatService seatService, TripService tripService, BookingService bookingService,
                           TicketRepository ticketRepository, PaymentRepository paymentRepository,
                           CarriageRepository carriageRepository, BookingRepository bookingRepository,
                           OrderRepository orderRepository, RouteRepository routeRepository,
                           SeatTypeRepository seatTypeRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.stationService = stationService;
        this.stationRepository = stationRepository;
        this.routeService = routeService;
        this.trainService = trainService;
        this.seatService = seatService;
        this.tripService = tripService;
        this.bookingService = bookingService;
        this.ticketRepository = ticketRepository;
        this.paymentRepository = paymentRepository;
        this.carriageRepository = carriageRepository;
        this.bookingRepository = bookingRepository;
        this.orderRepository = orderRepository;
        this.routeRepository = routeRepository;
        this.seatTypeRepository = seatTypeRepository;
    }

    @Override
    public void run(String... args) {

        System.out.println("--- Starting Realistic Data Initialization (LOGIC BẢN ĐỒ GHẾ) ---");
        LocalDate today = LocalDate.now();

        try {
            createUsers(today);
            createStations();
            createRoutes();
            createSeatTypes();
            createTrainsAndSeats();
            createTrips(today);
            createBookingsAndTickets();

            System.out.println("--- Realistic Data Initialization COMPLETE ---");

        } catch (Exception e) {
            System.err.println("Error during data initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createUsers(LocalDate today) {
        System.out.println("Creating Users...");
        User staff = new User();
        staff.setEmail("staff@example.com");
        staff.setPassword("password123");
        staff.setFullName("Nguyễn Văn B (Staff)");
        staff.setPhone("0911001100");
        staff.setCreateDate(today.minusMonths(6));
        staff.setRole(User.Role.STAFF);
        userService.addUser(staff);
        customer = new User();
        customer.setEmail("customer@example.com");
        customer.setPassword("password123");
        customer.setFullName("Trần Văn A (Customer)");
        customer.setPhone("0909009009");
        customer.setCreateDate(today.minusMonths(5));
        customer.setRole(User.Role.CUSTOMER);
        userService.addUser(customer);
    }

    private Station createStation(String code, String name, String city, String province, int distanceKm) {
        Station station = new Station();
        station.setCode(code);
        station.setName(name);
        station.setCity(city);
        station.setProvince(province);
        station.setDistanceKm(distanceKm);
        station.setStatus(Station.Status.ACTIVE);
        return stationService.createStation(station);
    }

    private void createStations() {
        System.out.println("Creating Stations (with KM)...");
        stationHaNoi = createStation("HNO", "Ga Hà Nội", "Hà Nội", "Hà Nội", 0);
        stationVinh = createStation("VIN", "Ga Vinh", "Nghệ An", "Nghệ An", 319);
        stationDaNang = createStation("DNG", "Ga Đà Nẵng", "Đà Nẵng", "Đà Nẵng", 791);
        stationNhaTrang = createStation("NTR", "Ga Nha Trang", "Khánh Hòa", "Khánh Hòa", 1315);
        stationSaiGon = createStation("SGO", "Ga Sài Gòn", "Hồ Chí Minh", "Hồ Chí Minh", 1726);
    }

    private Route createRoute(String code, Station start, Station end) {
        Route route = new Route();
        route.setCode(code);
        route.setStartStation(start);
        route.setEndStation(end);
        route.setStatus(Route.Status.ACTIVE);
        return routeService.createRoute(route);
    }

    private void createRoutes() {
        System.out.println("Creating sample Routes...");
        routeHnSg = createRoute("HN-SG", stationHaNoi, stationSaiGon);
        routeSgHn = createRoute("SG-HN", stationSaiGon, stationHaNoi);
        routeHnVn = createRoute("HN-VIN", stationHaNoi, stationVinh);
        routeHnDng = createRoute("HN-DNG", stationHaNoi, stationDaNang);
        routeDngSg = createRoute("DNG-SG", stationDaNang, stationSaiGon);
        routeSgNt = createRoute("SG-NT", stationSaiGon, stationNhaTrang);
    }

    private void createSeatTypes() {
        System.out.println("Creating Seat Types (Price per KM)...");
        SeatType vip = new SeatType();
        vip.setName("Giường nằm VIP (4 chỗ)");
        vip.setPricePerKm(BigDecimal.valueOf(1100));
        seatTypeVip = seatTypeRepository.save(vip);
        SeatType normal = new SeatType();
        normal.setName("Ngồi mềm điều hòa");
        normal.setPricePerKm(BigDecimal.valueOf(700));
        seatTypeNormal = seatTypeRepository.save(normal);
    }

    private Train createTrain(String code, String name) {
        Train train = new Train();
        train.setCode(code);
        train.setName(name);
        train.setStatus(TrainStatus.AVAILABLE);
        return trainService.saveTrain(train);
    }

    private void createTrainsAndSeats() {
        System.out.println("Creating Trains, Carriages, and Seats (Reduced Quantity)...");
        trainSE1 = createTrain("SE1", "Thống Nhất (HN-SG)");
        trainSE3 = createTrain("SE3", "Thống Nhất (HN-SG)");
        List<Train> allTrains = List.of(trainSE1, trainSE3);

        for (Train train : allTrains) {
            Carriage carriage = new Carriage();
            carriage.setTrain(train);
            carriage.setName("Toa 1 (VIP)");
            carriage.setType("Giường nằm 4 chỗ");
            carriage.setPosition(1);
            carriage.setSeatType(seatTypeVip);
            carriage = carriageRepository.save(carriage);
            for (int j = 1; j <= 10; j++) { // 10 ghế
                Seat seat = new Seat();
                seat.setCarriage(carriage);
                seat.setSeatNumber("A" + j);
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setIsActive(true);
                seat = seatService.saveSeat(seat);
                if (train.getCode().equals("SE1") && j == 1) se1_vip_seat_A1 = seat;
                if (train.getCode().equals("SE3") && j == 1) se3_vip_seat_A1 = seat;
            }

            Carriage carriage2 = new Carriage();
            carriage2.setTrain(train);
            carriage2.setName("Toa 2 (Thường)");
            carriage2.setType("Ngồi mềm điều hòa");
            carriage2.setPosition(2);
            carriage2.setSeatType(seatTypeNormal);
            carriage2 = carriageRepository.save(carriage2);
            for (int j = 1; j <= 20; j++) { // 20 ghế
                Seat seat = new Seat();
                seat.setCarriage(carriage2);
                seat.setSeatNumber("B" + j);
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setIsActive(true);
                seat = seatService.saveSeat(seat);
                if (train.getCode().equals("SE1") && j == 1) se1_normal_seat_B1 = seat;
            }
        }
    }

    private Trip createTrip(Train train, Route route, LocalDateTime departure, LocalDateTime arrival, TripStatus status) {
        Trip trip = new Trip();
        trip.setTrain(train);
        trip.setRoute(route);
        trip.setDepartureStation(route.getStartStation().getName());
        trip.setArrivalStation(route.getEndStation().getName());
        trip.setDepartureTime(departure);
        trip.setArrivalTime(arrival);
        trip.setStatus(status);
        return tripService.saveTrip(trip);
    }

    private void createTrips(LocalDate today) {
        System.out.println("Creating sample Trips (Schedule)...");
        tripSE1 = createTrip(trainSE1, routeHnSg,
                today.plusDays(7).atTime(19, 30),
                today.plusDays(9).atTime(4, 30),
                TripStatus.UPCOMING);
        tripSE3 = createTrip(trainSE3, routeHnSg,
                today.plusDays(8).atTime(22, 0),
                today.plusDays(10).atTime(7, 0),
                TripStatus.UPCOMING);
    }


    // ===== SỬA HÀM NÀY (THÊM DOB VÀ ID CARD) =====
    private void createBookingsAndTickets() {
        System.out.println("Creating sample Orders, Bookings, Tickets (Passenger Form Logic)...");

        // --- ĐƠN HÀNG 1: (SE1, 1 vé A1) - ĐÃ THANH TOÁN ---

        BookingRequest request1 = new BookingRequest();
        request1.setTripId(tripSE1.getTripId());

        PassengerInfo passenger1 = new PassengerInfo();
        passenger1.setSeatId(se1_vip_seat_A1.getSeatId());
        passenger1.setPassengerName(customer.getFullName());
        passenger1.setPhone(customer.getPhone());
        passenger1.setEmail(customer.getEmail());
        passenger1.setPassengerType("ADULT");
        passenger1.setDob(LocalDate.of(1990, 5, 15)); // THÊM NGÀY SINH (ADULT)
        passenger1.setPassengerIdCard("012345678901"); // THÊM CCCD
        request1.getPassengers().add(passenger1);

        Order order1 = bookingService.createOrder(request1, customer);

        Booking booking1 = order1.getBookings().get(0);
        Ticket ticket1 = new Ticket();
        ticket1.setCode("TICKET-SE1-A1");
        ticket1.setBooking(booking1);
        ticket1.setTrip(booking1.getTrip());
        ticket1.setSeat(booking1.getSeat());
        ticket1.setFromStation(stationHaNoi);
        ticket1.setToStation(stationSaiGon);
        ticket1.setPassengerName(customer.getFullName());
        ticket1.setPassengerPhone(customer.getPhone());

        // THÊM: Sao chép 2 trường mới sang Ticket
        ticket1.setPassengerIdCard(booking1.getPassengerIdCard());
        ticket1.setDob(booking1.getDob());

        ticket1.setTotalPrice(booking1.getPrice());
        ticket1.setStatus(TicketStatus.ACTIVE);
        ticket1.setBookedAt(LocalDateTime.now().minusDays(1));
        ticketRepository.save(ticket1);

        Payment payment1 = new Payment();
        payment1.setOrder(order1);
        payment1.setUser(customer);
        payment1.setAmount(order1.getTotalPrice());
        payment1.setStatus(PaymentStatus.SUCCESS);
        payment1.setTransactionRef("TXN_PAID_123");
        payment1.setOrderInfo("Thanh toán vé tàu (Data init)");
        payment1.setBankCode("VCB");
        payment1.setResponseCode("00");
        payment1.setPayDate(LocalDateTime.now().minusDays(1).plusMinutes(15));
        paymentRepository.save(payment1);

        order1.setStatus(PaymentStatus.SUCCESS);
        orderRepository.save(order1);
        for(Booking b : order1.getBookings()) {
            b.setStatus(BookingStatus.PAID);
            bookingRepository.save(b);
        }

        // --- ĐƠN HÀNG 2: (SE1, 1 vé B1) - CHƯA THANH TOÁN ---
        BookingRequest request2 = new BookingRequest();
        request2.setTripId(tripSE1.getTripId());
        PassengerInfo passenger2 = new PassengerInfo();
        passenger2.setSeatId(se1_normal_seat_B1.getSeatId());
        passenger2.setPassengerName("Người Đi Cùng");
        passenger2.setPhone(customer.getPhone());
        passenger2.setEmail(customer.getEmail());
        passenger2.setPassengerType("ADULT");
        passenger2.setDob(LocalDate.of(1995, 1, 1)); // THÊM NGÀY SINH (ADULT)
        passenger2.setPassengerIdCard("087654321"); // THÊM CCCD
        request2.getPassengers().add(passenger2);

        Order order2 = bookingService.createOrder(request2, customer);

        // --- ĐƠN HÀNG 3: (SE3, 1 vé A1) - CHƯA THANH TOÁN ---
        BookingRequest request3 = new BookingRequest();
        request3.setTripId(tripSE3.getTripId());
        PassengerInfo passenger3 = new PassengerInfo();
        passenger3.setSeatId(se3_vip_seat_A1.getSeatId());
        passenger3.setPassengerName(customer.getFullName());
        passenger3.setPhone(customer.getPhone());
        passenger3.setEmail(customer.getEmail());
        passenger3.setPassengerType("ADULT");
        passenger3.setDob(LocalDate.of(1990, 5, 15)); // THÊM NGÀY SINH (ADULT)
        passenger3.setPassengerIdCard("012345678901"); // THÊM CCCD
        request3.getPassengers().add(passenger3);

        Order order3 = bookingService.createOrder(request3, customer);
    }
}