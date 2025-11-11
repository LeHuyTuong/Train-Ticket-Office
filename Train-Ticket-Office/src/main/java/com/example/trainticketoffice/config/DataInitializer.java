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
import java.util.Map; // THÊM

/**
 * DataInitializer - Đã Gộp Logic (Bản đồ ghế)
 * Cập nhật (Giai đoạn 8.3) để dùng BookingRequest
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
    private final AdminWalletService adminWalletService; // <-- THÊM MỚI

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
                           SeatTypeRepository seatTypeRepository,
                           AdminWalletService adminWalletService) { // <-- THÊM THAM SỐ
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
        this.adminWalletService = adminWalletService; // <-- THÊM DÒNG NÀY
    }

    @Override
    public void run(String... args) {

        System.out.println("--- Starting Realistic Data Initialization (LOGIC BẢN ĐỒ GHẾ) ---");
        LocalDate today = LocalDate.now();

        try {
            // Khởi tạo ví Admin trước
            adminWalletService.initializeWallet(); // <-- THÊM DÒNG NÀY

            createUsers(today);
            createStations();
            createRoutes(); // Tạo 6 tuyến mẫu
            // createBulkRoutes(); // XÓA: Gây treo
            createSeatTypes(); // Tạo 2 loại ghế (VIP, Thường)
            createTrainsAndSeats(); // Tạo 6 tàu, toa, và các ghế A1, B1...
            createTrips(today); // Tạo 2 chuyến mẫu
            // createBulkTrips(today); // XÓA: Gây treo

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

    // (Hàm tạo 6 tuyến mẫu)
    private void createRoutes() {
        System.out.println("Creating sample Routes...");
        routeHnSg = createRoute("HN-SG", stationHaNoi, stationSaiGon);
        routeSgHn = createRoute("SG-HN", stationSaiGon, stationHaNoi);
        routeHnVn = createRoute("HN-VIN", stationHaNoi, stationVinh);
        routeHnDng = createRoute("HN-DNG", stationHaNoi, stationDaNang);
        routeDngSg = createRoute("DNG-SG", stationDaNang, stationSaiGon);
        routeSgNt = createRoute("SG-NT", stationSaiGon, stationNhaTrang);
    }

    // (Hàm tạo Loại ghế)
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
    // (Hàm tạo Tàu, Toa, Ghế) - ĐÃ SỬA: Tạo 6 tàu
    private void createTrainsAndSeats() {
        System.out.println("Creating Trains, Carriages, and Seats (6 Trains)...");

        // 1. Khởi tạo tất cả 6 tàu
        trainSE1 = createTrain("SE1", "Thống Nhất (HN-SG)");
        trainSE2 = createTrain("SE2", "Thống Nhất (SG-HN)"); // Tàu về
        trainSE3 = createTrain("SE3", "Thống Nhất (HN-SG)");
        trainSE4 = createTrain("SE4", "Thống Nhất (SG-HN)"); // Tàu về
        trainSE5 = createTrain("SE5", "Tàu nhanh (HN-SG)");
        trainSE6 = createTrain("SE6", "Tàu nhanh (SG-HN)"); // Tàu về

        // 2. Đưa tất cả 6 tàu vào danh sách
        List<Train> allTrains = List.of(trainSE1, trainSE2, trainSE3, trainSE4, trainSE5, trainSE6);

        // 3. Vòng lặp này sẽ TỰ ĐỘNG tạo toa/ghế cho cả 6 tàu
        for (Train train : allTrains) {
            // 1 Toa VIP
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

                // Gán biến cho các ghế mẫu (dùng cho booking)
                if (train.getCode().equals("SE1") && j == 1) se1_vip_seat_A1 = seat;
                if (train.getCode().equals("SE3") && j == 1) se3_vip_seat_A1 = seat;
            }

            // 2 Toa Thường
            for (int i = 1; i <= 2; i++) {
                int toaPos = i + 1;
                Carriage carriage2 = new Carriage();
                carriage2.setTrain(train);
                carriage2.setName("Toa " + toaPos + " (Thường)");
                carriage2.setType("Ngồi mềm điều hòa");
                carriage2.setPosition(toaPos);
                carriage2.setSeatType(seatTypeNormal);
                carriage2 = carriageRepository.save(carriage2);
                for (int j = 1; j <= 20; j++) { // 20 ghế
                    Seat seat = new Seat();
                    seat.setCarriage(carriage2);
                    seat.setSeatNumber("B" + j);
                    seat.setStatus(SeatStatus.AVAILABLE);
                    seat.setIsActive(true);
                    seat = seatService.saveSeat(seat);

                    // Gán biến cho ghế mẫu (dùng cho booking)
                    if (train.getCode().equals("SE1") && i == 1 && j == 1) se1_normal_seat_B1 = seat;
                }
            }
        }

        System.out.println("Total trains created: " + allTrains.size());
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

    // (Hàm tạo chuyến) - ĐÃ SỬA: Dùng 6 tàu (3 đi, 3 về)
    private void createTrips(LocalDate today) {
        System.out.println("Creating bulk Trips (Schedule using 6 trains)...");

        // Cấu hình: 100 ngày, mỗi ngày 6 chuyến (3 đi, 3 về) = 600 chuyến
        int DAYS_TO_GENERATE = 100;

        for (int i = 1; i <= DAYS_TO_GENERATE; i++) {
            // Lấy ngày khởi hành cho vòng lặp này
            LocalDate departureDate = today.plusDays(i);

            // --- TẠO CHUYẾN ĐI (HN -> SG) ---
            // (Dùng tàu SE1, SE3, SE5 cho tuyến routeHnSg)

            // Chuyến 1: Tàu SE1 (HN-SG) - 19:30
            LocalDateTime depTimeSE1 = departureDate.atTime(19, 30);
            LocalDateTime arrTimeSE1 = departureDate.plusDays(2).atTime(4, 30);
            Trip t_SE1_HNSG = createTrip(trainSE1, routeHnSg, depTimeSE1, arrTimeSE1, TripStatus.UPCOMING);

            // Chuyến 2: Tàu SE3 (HN-SG) - 22:00
            LocalDateTime depTimeSE3 = departureDate.atTime(22, 0);
            LocalDateTime arrTimeSE3 = departureDate.plusDays(2).atTime(7, 0);
            Trip t_SE3_HNSG = createTrip(trainSE3, routeHnSg, depTimeSE3, arrTimeSE3, TripStatus.UPCOMING);

            // Chuyến 3: Tàu SE5 (HN-SG) - 09:00 (Tàu nhanh)
            LocalDateTime depTimeSE5 = departureDate.atTime(9, 0);
            LocalDateTime arrTimeSE5 = departureDate.plusDays(1).atTime(18, 0); // Giả sử tàu này đi nhanh hơn
            createTrip(trainSE5, routeHnSg, depTimeSE5, arrTimeSE5, TripStatus.UPCOMING);


            // --- TẠO CHUYẾN VỀ (KHỨ HỒI) (SG -> HN) ---
            // (Dùng tàu SE2, SE4, SE6 cho tuyến routeSgHn)

            // Chuyến 4: Tàu SE2 (SG-HN) - 19:30
            LocalDateTime depTimeSE2_Return = departureDate.atTime(19, 30);
            LocalDateTime arrTimeSE2_Return = departureDate.plusDays(2).atTime(4, 30);
            createTrip(trainSE2, routeSgHn, depTimeSE2_Return, arrTimeSE2_Return, TripStatus.UPCOMING);

            // Chuyến 5: Tàu SE4 (SG-HN) - 22:00
            LocalDateTime depTimeSE4_Return = departureDate.atTime(22, 0);
            LocalDateTime arrTimeSE4_Return = departureDate.plusDays(2).atTime(7, 0);
            createTrip(trainSE4, routeSgHn, depTimeSE4_Return, arrTimeSE4_Return, TripStatus.UPCOMING);

            // Chuyến 6: Tàu SE6 (SG-HN) - 09:00 (Tàu nhanh)
            LocalDateTime depTimeSE6_Return = departureDate.atTime(9, 0);
            LocalDateTime arrTimeSE6_Return = departureDate.plusDays(1).atTime(18, 0);
            createTrip(trainSE6, routeSgHn, depTimeSE6_Return, arrTimeSE6_Return, TripStatus.UPCOMING);


            // --- QUAN TRỌNG: GÁN 2 CHUYẾN MẪU ĐẦU TIÊN ---
            // (Dùng cho createBookingsAndTickets())
            if (i == 1) {
                System.out.println("Assigning first trips (day " + i + ") for sample bookings...");
                this.tripSE1 = t_SE1_HNSG; // Gán chuyến SE1 (HN-SG)
                this.tripSE3 = t_SE3_HNSG; // Gán chuyến SE3 (HN-SG)
            }
        }

        // CẬP NHẬT VÍ ADMIN (GIẢ LẬP ĐÃ THANH TOÁN)

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
