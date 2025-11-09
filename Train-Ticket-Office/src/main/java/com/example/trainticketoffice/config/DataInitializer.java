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
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;
    private final UserRepository userRepository;
    private final StationService stationService;
    private final StationRepository stationRepository;
    private final RouteService routeService;
    private final TrainService trainService;
    private final SeatService seatService; // <-- KHÔI PHỤC
    private final TripService tripService;
    private final BookingService bookingService;
    private final TicketRepository ticketRepository;
    private final PaymentRepository paymentRepository;
    private final CarriageRepository carriageRepository;
    private final BookingRepository bookingRepository;
    private final OrderRepository orderRepository;
    private final SeatTypeRepository seatTypeRepository; // (Giữ lại)

    // Các biến tạm để chia sẻ giữa các hàm
    private User customer;
    private Station stationHaNoi, stationVinh, stationHue, stationDaNang, stationNhaTrang, stationSaiGon;
    private Route routeHnSg, routeSgHn, routeHnVn, routeHnDng, routeDngSg, routeSgNt;
    private Train trainSE1, trainSE2, trainSE3, trainSE4, trainSE5, trainSE6;
    private Seat se1_vip_seat_A1, se1_normal_seat_B1, se3_vip_seat_A1; // <-- KHÔI PHỤC
    private Trip tripSE1, tripSE3;
    private SeatType seatTypeVip, seatTypeNormal;
    // private Carriage se1_carriage_vip1, se1_carriage_normal1, se3_carriage_vip1; // (Không cần nữa)


    // SỬA: Thêm SeatService, SeatTypeRepository
    public DataInitializer(UserService userService, UserRepository userRepository, StationService stationService, StationRepository stationRepository, RouteService routeService, TrainService trainService, SeatService seatService, TripService tripService, BookingService bookingService, TicketRepository ticketRepository, PaymentRepository paymentRepository, CarriageRepository carriageRepository, BookingRepository bookingRepository, OrderRepository orderRepository, SeatTypeRepository seatTypeRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.stationService = stationService;
        this.stationRepository = stationRepository;
        this.routeService = routeService;
        this.trainService = trainService;
        this.seatService = seatService; // <-- KHÔI PHỤC
        this.tripService = tripService;
        this.bookingService = bookingService;
        this.ticketRepository = ticketRepository;
        this.paymentRepository = paymentRepository;
        this.carriageRepository = carriageRepository;
        this.bookingRepository = bookingRepository;
        this.orderRepository = orderRepository;
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
            createTrainsAndSeats(); // SỬA (Tên hàm cũ)
            createTrips(today);
            createBookingsAndTickets(); // SỬA

            System.out.println("--- Realistic Data Initialization COMPLETE ---");

        } catch (Exception e) {
            System.err.println("Error during data initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createUsers(LocalDate today) {
        // (Giữ nguyên)
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
        // (Giữ nguyên logic thêm KM)
        System.out.println("Creating 34 Stations (with KM)...");
        stationHaNoi = createStation("HNO", "Ga Hà Nội", "Hà Nội", "Hà Nội", 0);
        createStation("GBA", "Ga Giáp Bát", "Hà Nội", "Hà Nội", 5);
        createStation("PLY", "Ga Phủ Lý", "Hà Nam", "Hà Nam", 56);
        createStation("NDH", "Ga Nam Định", "Nam Định", "Nam Định", 87);
        createStation("NBH", "Ga Ninh Bình", "Ninh Bình", "Ninh Bình", 115);
        createStation("BSN", "Ga Bỉm Sơn", "Thanh Hóa", "Thanh Hóa", 141);
        createStation("THO", "Ga Thanh Hóa", "Thanh Hóa", "Thanh Hóa", 175);
        stationVinh = createStation("VIN", "Ga Vinh", "Nghệ An", "Nghệ An", 319);
        stationDaNang = createStation("DNG", "Ga Đà Nẵng", "Đà Nẵng", "Đà Nẵng", 791);
        stationNhaTrang = createStation("NTR", "Ga Nha Trang", "Khánh Hòa", "Khánh Hòa", 1315);
        stationSaiGon = createStation("SGO", "Ga Sài Gòn", "Hồ Chí Minh", "Hồ Chí Minh", 1726);
        // (Thêm các ga khác nếu cần)
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
        // (Giữ nguyên)
        System.out.println("Creating Routes...");
        routeHnSg = createRoute("HN-SG", stationHaNoi, stationSaiGon);
        routeSgHn = createRoute("SG-HN", stationSaiGon, stationHaNoi);
        routeHnVn = createRoute("HN-VIN", stationHaNoi, stationVinh);
        routeHnDng = createRoute("HN-DNG", stationHaNoi, stationDaNang);
        routeDngSg = createRoute("DNG-SG", stationDaNang, stationSaiGon);
        routeSgNt = createRoute("SG-NT", stationSaiGon, stationNhaTrang);
    }

    private void createSeatTypes() {
        // (Giữ nguyên)
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

    // ===== SỬA HOÀN TOÀN HÀM NÀY (KHÔI PHỤC LOGIC TẠO GHẾ A1, A2...) =====
    private void createTrainsAndSeats() {
        System.out.println("Creating Trains, Carriages, and Seats (Seat Map Logic)...");
        trainSE1 = createTrain("SE1", "Thống Nhất (HN-SG)");
        trainSE2 = createTrain("SE2", "Thống Nhất (SG-HN)");
        trainSE3 = createTrain("SE3", "Thống Nhất (HN-SG)");
        trainSE4 = createTrain("SE4", "Thống Nhất (SG-HN)");
        trainSE5 = createTrain("SE5", "Thống Nhất Tối (HN-SG)");
        trainSE6 = createTrain("SE6", "Thống Nhất Tối (SG-HN)");

        List<Train> allTrains = List.of(trainSE1, trainSE2, trainSE3, trainSE4, trainSE5, trainSE6);

        for (Train train : allTrains) {
            // 2 Toa VIP
            for (int i = 1; i <= 2; i++) {
                Carriage carriage = new Carriage();
                carriage.setTrain(train);
                carriage.setName("Toa " + i + " (VIP)");
                carriage.setType("Giường nằm 4 chỗ");
                carriage.setPosition(i);
                carriage.setSeatType(seatTypeVip); // Gán Loại Ghế
                // (Không set capacity)
                carriage = carriageRepository.save(carriage);

                // TẠO GHẾ BÊN TRONG TOA
                for (int j = 1; j <= 20; j++) {
                    Seat seat = new Seat();
                    seat.setCarriage(carriage);
                    seat.setSeatNumber("A" + j);
                    // (Không set giá, không set loại ở đây)
                    seat.setStatus(SeatStatus.AVAILABLE);
                    seat.setIsActive(true);
                    seat = seatService.saveSeat(seat); // Dùng SeatService

                    if (train.getCode().equals("SE1") && i == 1 && j == 1) se1_vip_seat_A1 = seat;
                    if (train.getCode().equals("SE3") && i == 1 && j == 1) se3_vip_seat_A1 = seat;
                }
            }

            // 5 Toa Thường
            for (int i = 1; i <= 5; i++) {
                int toaPos = i + 2;
                Carriage carriage = new Carriage();
                carriage.setTrain(train);
                carriage.setName("Toa " + toaPos + " (Thường)");
                carriage.setType("Ngồi mềm điều hòa");
                carriage.setPosition(toaPos);
                carriage.setSeatType(seatTypeNormal);
                carriage = carriageRepository.save(carriage);

                // TẠO GHẾ BÊN TRONG TOA
                for (int j = 1; j <= 50; j++) {
                    Seat seat = new Seat();
                    seat.setCarriage(carriage);
                    seat.setSeatNumber("B" + j);
                    seat.setStatus(SeatStatus.AVAILABLE);
                    seat.setIsActive(true);
                    seat = seatService.saveSeat(seat);

                    if (train.getCode().equals("SE1") && i == 1 && j == 1) se1_normal_seat_B1 = seat;
                }
            }
        }
    }
    // ==========================================================

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
        // (Giữ nguyên)
        System.out.println("Creating Trips (Schedule)...");
        tripSE1 = createTrip(trainSE1, routeHnSg,
                today.plusDays(7).atTime(19, 30),
                today.plusDays(9).atTime(4, 30),
                TripStatus.UPCOMING);
        tripSE3 = createTrip(trainSE3, routeHnSg,
                today.plusDays(8).atTime(22, 0),
                today.plusDays(10).atTime(7, 0),
                TripStatus.UPCOMING);
        // (Thêm các chuyến khác nếu cần)
    }

    // ===== SỬA HOÀN TOÀN HÀM NÀY (ĐỂ DÙNG LOGIC BẢN ĐỒ GHẾ) =====
    private void createBookingsAndTickets() {
        System.out.println("Creating sample Orders, Bookings, Tickets (Seat Map Logic)...");

        // --- ĐƠN HÀNG 1: (SE1, 1 vé A1) - ĐÃ THANH TOÁN ---
        Order order1 = bookingService.createOrder(
                customer.getId(),
                tripSE1.getTripId(),
                List.of(se1_vip_seat_A1.getSeatId()), // Truyền List<SeatId>
                customer.getFullName(),
                "ADULT",
                customer.getPhone(),
                customer.getEmail()
        );

        Booking booking1 = order1.getBookings().get(0);

        Ticket ticket1 = new Ticket();
        ticket1.setCode("TICKET-SE1-A1");
        ticket1.setBooking(booking1);
        ticket1.setTrip(booking1.getTrip());

        // SỬA: Dùng Seat
        ticket1.setSeat(booking1.getSeat());
        // (Không cần setCarriage, seatNumber)

        ticket1.setFromStation(stationHaNoi);
        ticket1.setToStation(stationSaiGon);
        ticket1.setPassengerName(customer.getFullName());
        ticket1.setPassengerPhone(customer.getPhone());
        ticket1.setPassengerIdCard("012345678901");
        ticket1.setTotalPrice(booking1.getPrice()); // Giá đã được Service tính
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
        Order order2 = bookingService.createOrder(
                customer.getId(),
                tripSE1.getTripId(),
                List.of(se1_normal_seat_B1.getSeatId()),
                "Người Đi Cùng",
                "ADULT",
                customer.getPhone(),
                customer.getEmail()
        );

        // --- ĐƠN HÀNG 3: (SE3, 1 vé A1) - CHƯA THANH TOÁN ---
        Order order3 = bookingService.createOrder(
                customer.getId(),
                tripSE3.getTripId(),
                List.of(se3_vip_seat_A1.getSeatId()),
                customer.getFullName(),
                "ADULT",
                customer.getPhone(),
                customer.getEmail()
        );
    }
}