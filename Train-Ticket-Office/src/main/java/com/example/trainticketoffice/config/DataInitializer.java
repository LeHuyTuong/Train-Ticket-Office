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

/**
 * DataInitializer phiên bản "đời thật".
 * Tạo 34 Ga, 6 Tàu (hàng ngàn ghế), 6 Chuyến, và 3 Bookings mẫu.
 * **MỚI**: Tự động tạo hàng loạt tuyến đường (routes) khứ hồi và hàng ngàn chuyến đi (trips)
 * cho 14 ngày tới.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;
    private final UserRepository userRepository;
    private final StationService stationService;
    private final StationRepository stationRepository;
    private final RouteService routeService;
    private final RouteRepository routeRepository; // <-- THÊM
    private final TrainService trainService;
    private final SeatService seatService;
    private final TripService tripService;
    private final BookingService bookingService;
    private final TicketRepository ticketRepository;
    private final PaymentRepository paymentRepository;
    private final CarriageRepository carriageRepository;
    private final BookingRepository bookingRepository;
    private final OrderRepository orderRepository;

    // Các biến tạm để chia sẻ giữa các hàm
    private User customer;
    private Station stationHaNoi, stationVinh, stationHue, stationDaNang, stationNhaTrang, stationSaiGon;
    private Route routeHnSg, routeSgHn, routeHnVn, routeHnDng, routeDngSg, routeSgNt;
    private Train trainSE1, trainSE2, trainSE3, trainSE4, trainSE5, trainSE6;
    private Seat se1_vip_seat_A1, se1_normal_seat_B1, se3_vip_seat_A1;
    private Trip tripSE1, tripSE3;

    // SỬA: Thêm RouteRepository vào constructor
    public DataInitializer(UserService userService, UserRepository userRepository, StationService stationService, StationRepository stationRepository, RouteService routeService, TrainService trainService, SeatService seatService, TripService tripService, BookingService bookingService, TicketRepository ticketRepository, PaymentRepository paymentRepository, CarriageRepository carriageRepository, BookingRepository bookingRepository, OrderRepository orderRepository, RouteRepository routeRepository /* <--- THÊM */) {
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
        this.routeRepository = routeRepository; // <-- THÊM
    }

    @Override
    public void run(String... args) {

        System.out.println("--- Starting Realistic Data Initialization ---");

        LocalDate today = LocalDate.now();

        try {
            createUsers(today);
            createStations();
            createRoutes();
            createBulkRoutes(); // TẠO HÀNG LOẠT ROUTE KHỨ HỒI
            createTrainsAndSeats();
            createTrips(today);
            createBulkTrips(today); // TẠO HÀNG LOẠT TRIP
            createBookingsAndTickets(); // <-- SỬA LOGIC TRONG HÀM NÀY

            System.out.println("--- Realistic Data Initialization COMPLETE ---");

        } catch (Exception e) {
            System.err.println("Error during data initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // (Các hàm createUsers, createStations, createRoutes, createTrainsAndSeats, createTrip, createTrips giữ nguyên)
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

    private Station createStation(String code, String name, String city, String province) {
        Station station = new Station();
        station.setCode(code);
        station.setName(name);
        station.setCity(city);
        station.setProvince(province);
        station.setStatus(Station.Status.ACTIVE);
        return stationService.createStation(station);
    }

    private void createStations() {
        System.out.println("Creating 34 Stations...");
        stationHaNoi = createStation("HNO", "Ga Hà Nội", "Hà Nội", "Hà Nội");
        createStation("GBA", "Ga Giáp Bát", "Hà Nội", "Hà Nội");
        createStation("PLY", "Ga Phủ Lý", "Hà Nam", "Hà Nam");
        createStation("NDH", "Ga Nam Định", "Nam Định", "Nam Định");
        createStation("NBH", "Ga Ninh Bình", "Ninh Bình", "Ninh Bình");
        createStation("BSN", "Ga Bỉm Sơn", "Thanh Hóa", "Thanh Hóa");
        createStation("THO", "Ga Thanh Hóa", "Thanh Hóa", "Thanh Hóa");
        stationVinh = createStation("VIN", "Ga Vinh", "Nghệ An", "Nghệ An");
        createStation("YTR", "Ga Yên Trung", "Hà Tĩnh", "Hà Tĩnh");
        createStation("HPH", "Ga Hương Phố", "Hà Tĩnh", "Hà Tĩnh");
        createStation("DLI", "Ga Đồng Lê", "Quảng Bình", "Quảng Bình");
        createStation("DHO", "Ga Đồng Hới", "Quảng Bình", "Quảng Bình");
        createStation("DHA", "Ga Đông Hà", "Quảng Trị", "Quảng Trị");
        stationHue = createStation("HUE", "Ga Huế", "Thừa Thiên Huế", "Thừa Thiên Huế");
        createStation("LCO", "Ga Lăng Cô", "Thừa Thiên Huế", "Thừa Thiên Huế");
        stationDaNang = createStation("DNG", "Ga Đà Nẵng", "Đà Nẵng", "Đà Nẵng");
        createStation("TKY", "Ga Trà Kiệu", "Quảng Nam", "Quảng Nam");
        createStation("PCG", "Ga Phú Cang", "Quảng Nam", "Quảng Nam");
        createStation("TKI", "Ga Tam Kỳ", "Quảng Nam", "Quảng Nam");
        createStation("NUI", "Ga Núi Thành", "Quảng Nam", "Quảng Nam");
        createStation("QNG", "Ga Quảng Ngãi", "Quảng Ngãi", "Quảng Ngãi");
        createStation("DTR", "Ga Diêu Trì", "Bình Định", "Bình Định");
        createStation("THO", "Ga Tuy Hòa", "Phú Yên", "Phú Yên");
        createStation("GHA", "Ga Giã", "Khánh Hòa", "Khánh Hòa");
        stationNhaTrang = createStation("NTR", "Ga Nha Trang", "Khánh Hòa", "Khánh Hòa");
        createStation("TCH", "Ga Tháp Chàm", "Ninh Thuận", "Ninh Thuận");
        createStation("SMA", "Ga Sông Mao", "Bình Thuận", "Bình Thuận");
        createStation("BTH", "Ga Bình Thuận", "Bình Thuận", "Bình Thuận"); // (Ga Mương Mán cũ)
        createStation("LKH", "Ga Long Khánh", "Đồng Nai", "Đồng Nai");
        createStation("BHO", "Ga Biên Hòa", "Đồng Nai", "Đồng Nai");
        createStation("DAN", "Ga Dĩ An", "Bình Dương", "Bình Dương");
        stationSaiGon = createStation("SGO", "Ga Sài Gòn", "Hồ Chí Minh", "Hồ Chí Minh");
        createStation("PYG", "Ga Chợ Gã", "Nghệ An", "Nghệ An");
        createStation("MIN", "Ga Minh Khôi", "Thanh Hóa", "Thanh Hóa");
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

    /**
     * HÀM MỚI: Tạo hàng loạt tuyến đường khứ hồi cho tất cả các ga.
     */
    private void createBulkRoutes() {
        System.out.println("Creating Bulk Routes (Round Trips)...");
        List<Station> allStations = stationRepository.findAll();
        if (allStations.size() < 2) {
            System.out.println("Not enough stations to create bulk routes.");
            return;
        }
        int routesCreated = 0;
        for (int i = 0; i < allStations.size(); i++) {
            for (int j = 0; j < allStations.size(); j++) {
                if (i == j) continue; // Bỏ qua tuyến đường đến chính nó

                Station start = allStations.get(i);
                Station end = allStations.get(j);
                String routeCode = start.getCode() + "-" + end.getCode();

                // Service nên xử lý việc trùng lặp code, ở đây ta cứ tạo
                createRoute(routeCode, start, end);
                routesCreated++;
            }
        }
        System.out.println("Created " + routesCreated + " bulk routes.");
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
        trainSE2 = createTrain("SE2", "Thống Nhất (SG-HN)");
        trainSE3 = createTrain("SE3", "Thống Nhất (HN-SG)");
        trainSE4 = createTrain("SE4", "Thống Nhất (SG-HN)");
        trainSE5 = createTrain("SE5", "Thống Nhất Tối (HN-SG)");
        trainSE6 = createTrain("SE6", "Thống Nhất Tối (SG-HN)");

        List<Train> allTrains = List.of(trainSE1, trainSE2, trainSE3, trainSE4, trainSE5, trainSE6);

        for (Train train : allTrains) {
            // SỬA: Giảm số toa VIP (chỉ 1 toa)
            for (int i = 1; i <= 1; i++) {
                Carriage carriage = new Carriage();
                carriage.setTrain(train);
                carriage.setName("Toa " + i + " (VIP)");
                carriage.setType("Giường nằm 4 chỗ");
                carriage.setPosition(i);
                carriage = carriageRepository.save(carriage);
                // SỬA: Giảm số ghế VIP (chỉ 10 ghế)
                for (int j = 1; j <= 10; j++) {
                    Seat seat = new Seat();
                    seat.setCarriage(carriage);
                    seat.setSeatNumber("A" + j);
                    seat.setSeatType("VIP");
                    seat.setPrice(new BigDecimal("1800000.00"));
                    seat.setStatus(SeatStatus.AVAILABLE);
                    seat.setIsActive(true);
                    seat = seatService.saveSeat(seat);
                    if (train.getCode().equals("SE1") && i == 1 && j == 1) se1_vip_seat_A1 = seat;
                    if (train.getCode().equals("SE3") && i == 1 && j == 1) se3_vip_seat_A1 = seat;
                }
            }
            // SỬA: Giảm số toa thường (chỉ 2 toa)
            for (int i = 1; i <= 2; i++) {
                int toaPos = i + 1; // SỬA: vị trí 2, 3
                Carriage carriage = new Carriage();
                carriage.setTrain(train);
                carriage.setName("Toa " + toaPos + " (Thường)");
                carriage.setType("Ngồi mềm điều hòa");
                carriage.setPosition(toaPos);
                carriage = carriageRepository.save(carriage);
                // SỬA: Giảm số ghế thường (chỉ 20 ghế)
                for (int j = 1; j <= 20; j++) {
                    Seat seat = new Seat();
                    seat.setCarriage(carriage);
                    seat.setSeatNumber("B" + j);
                    seat.setSeatType("normal");
                    seat.setPrice(new BigDecimal("1100000.00"));
                    seat.setStatus(SeatStatus.AVAILABLE);
                    seat.setIsActive(true);
                    seat = seatService.saveSeat(seat);
                    if (train.getCode().equals("SE1") && i == 1 && j == 1) se1_normal_seat_B1 = seat;
                }
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

    /**
     * HÀM MỚI (Overload): Dùng cho createBulkTrips để tránh LazyInitializationException
     * Nhận tên ga (đã được tải) thay vì truy cập proxy 'route.getStartStation().getName()'
     */
    private Trip createTrip(Train train, Route route, LocalDateTime departure, LocalDateTime arrival, TripStatus status, String startStationName, String endStationName) {
        Trip trip = new Trip();
        trip.setTrain(train);
        trip.setRoute(route);
        trip.setDepartureStation(startStationName); // <-- SỬA: Dùng tên đã được tải
        trip.setArrivalStation(endStationName); // <-- SỬA: Dùng tên đã được tải
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
        createTrip(trainSE2, routeSgHn,
                today.plusDays(7).atTime(19, 30),
                today.plusDays(9).atTime(4, 30),
                TripStatus.UPCOMING);
        tripSE3 = createTrip(trainSE3, routeHnSg,
                today.plusDays(8).atTime(22, 0),
                today.plusDays(10).atTime(7, 0),
                TripStatus.UPCOMING);
        createTrip(trainSE4, routeSgHn,
                today.plusDays(8).atTime(22, 0),
                today.plusDays(10).atTime(7, 0),
                TripStatus.UPCOMING);
        createTrip(trainSE5, routeHnDng,
                today.plusDays(5).atTime(9, 0),
                today.plusDays(6).atTime(1, 0),
                TripStatus.UPCOMING);
        createTrip(trainSE6, routeSgNt,
                today.plusDays(6).atTime(10, 0),
                today.plusDays(6).atTime(18, 0),
                TripStatus.UPCOMING);
    }

    /**
     * HÀM MỚI: Tạo hàng loạt chuyến đi cho 30 ngày tới.
     */
    private void createBulkTrips(LocalDate today) {
        System.out.println("Creating Bulk Trips for 30 days..."); // SỬA: log 30 ngày
        List<Route> allRoutes = routeRepository.findAll();
        List<Train> allTrains = List.of(trainSE1, trainSE2, trainSE3, trainSE4, trainSE5, trainSE6);

        if (allRoutes.isEmpty() || allTrains.isEmpty()) {
            System.out.println("No routes or trains available for bulk trip creation.");
            return;
        }

        int tripsCreated = 0;
        int trainIndex = 0;
        LocalDateTime now = LocalDateTime.now(); // Lấy thời gian hiện tại

        // SỬA: Tạo chuyến đi cho 30 ngày tới, BẮT ĐẦU TỪ HÔM NAY
        for (int dayOffset = 0; dayOffset <= 30; dayOffset++) { // SỬA: 30 ngày (từ 14)

            // SỬA: Reset trainIndex MỖI NGÀY
            trainIndex = 0;

            for (Route route : allRoutes) {
                // Gán tàu xoay vòng
                Train train = allTrains.get(trainIndex % allTrains.size());
                trainIndex++;

                // ===== SỬA LỖI LazyInitializationException =====
                // 'route' object chứa 'Station' proxies mà session đã bị đóng.
                // Chúng ta phải chủ động tải lại (re-fetch) 'Station' thật bằng ID.
                // Lấy ID từ proxy (an toàn)
                Station startStation, endStation;
                try {
                    // SỬA: Đổi Long sang Integer để khớp với kiểu ID của Station
                    Integer startStationId = route.getStartStation().getId();
                    Integer endStationId = route.getEndStation().getId();

                    // Dùng repository để lấy object thật
                    // (Giờ startStationId và endStationId đã là Integer, sẽ khớp với findById)
                    startStation = stationRepository.findById(startStationId).orElse(null);
                    endStation = stationRepository.findById(endStationId).orElse(null);

                    if (startStation == null || endStation == null) {
                        System.err.println("Skipping bulk trip: Cannot find stations for route " + route.getCode());
                        continue; // Bỏ qua nếu không tìm thấy ga
                    }
                } catch (Exception e) {
                    System.err.println("Error fetching stations for bulk trip: " + e.getMessage());
                    continue; // Bỏ qua nếu có lỗi
                }
                // ===============================================


                // Ước tính thời gian di chuyển (giả lập đơn giản)
                long durationHours = 8; // Mặc định
                try {
                    // Dùng ID của ga để ước tính khoảng cách
                    // SỬA: Dùng 'startStation' và 'endStation' thật, không dùng proxy
                    long idDiff = Math.abs(startStation.getId() - endStation.getId());
                    if (idDiff == 0) durationHours = 1; // Tuyến nội thành (không nên xảy ra)
                    else if (idDiff <= 5) durationHours = 4; // Tuyến ngắn
                    else if (idDiff <= 15) durationHours = 12; // Tuyến trung bình
                    else durationHours = 32; // Tuyến dài (Bắc-Nam)
                } catch (Exception e) {
                    // Bỏ qua nếu ga bị null
                    durationHours = 8;
                }

                // Tạo 2 chuyến mỗi ngày (sáng, tối)
                LocalDate tripDate = today.plusDays(dayOffset); // Ngày diễn ra chuyến đi

                // ===== SỬA LỖI TÀU KHÔNG RẢNH =====
                // Thêm "jitter" (độ trễ) vài phút vào mỗi chuyến đi
                // để không có 2 tàu nào bị gán cùng 1 thời điểm chính xác
                long jitterMinutes = trainIndex;
                // =================================

                // SỬA: Đổi .plusMinutes() thành .plusSeconds()
                LocalDateTime departure1 = tripDate.atTime(7, 30).plusSeconds(jitterMinutes); // 7:30 + N giây
                LocalDateTime arrival1 = departure1.plusHours(durationHours);

                // SỬA: Chỉ tạo chuyến nếu thời gian khởi hành là trong tương lai
                if (departure1.isAfter(now)) {
                    // SỬA: Gọi hàm createTrip mới, truyền tên ga đã được tải
                    createTrip(train, route, departure1, arrival1, TripStatus.UPCOMING, startStation.getName(), endStation.getName());
                    tripsCreated++;
                }

                // SỬA: Đổi .plusMinutes() thành .plusSeconds()
                LocalDateTime departure2 = tripDate.atTime(21, 0).plusSeconds(jitterMinutes); // 21:00 + N giây
                LocalDateTime arrival2 = departure2.plusHours(durationHours);

                // SỬA: Chỉ tạo chuyến nếu thời gian khởi hành là trong tương lai
                if (departure2.isAfter(now)) {
                    // SỬA: Gọi hàm createTrip mới, truyền tên ga đã được tải
                    createTrip(train, route, departure2, arrival2, TripStatus.UPCOMING, startStation.getName(), endStation.getName());
                    tripsCreated++;
                }

                // SỬA: XÓA DÒNG NÀY (VÌ ĐÃ TĂNG Ở TRÊN RỒI)
                // trainIndex++;
            }
        }
        System.out.println("Created " + tripsCreated + " bulk trips.");
    }


    // ===== HÀM NÀY GIỮ NGUYÊN (VÌ NÓ DỰA VÀO CÁC BIẾN MẪU) =====
    private void createBookingsAndTickets() {
        System.out.println("Creating sample Orders, Bookings, Tickets, and Payments...");

        // --- ĐƠN HÀNG 1: (SE1, Ghế A1) - ĐÃ THANH TOÁN ---
        Order order1 = bookingService.createOrder(
                customer.getId(),
                tripSE1.getTripId(),
                List.of(se1_vip_seat_A1.getSeatId()), // Phải là List
                customer.getFullName(),
                "ADULT", // Thêm loại hành khách
                customer.getPhone(),
                customer.getEmail()
        );
        // Lấy booking ra từ order
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
        ticket1.setPassengerIdCard("012345678901");
        ticket1.setTotalPrice(booking1.getPrice());
        ticket1.setStatus(TicketStatus.ACTIVE);
        ticket1.setBookedAt(LocalDateTime.now().minusDays(1));
        ticketRepository.save(ticket1);

        Payment payment1 = new Payment();
        payment1.setOrder(order1); // SỬA: setOrder
        payment1.setUser(customer);
        payment1.setAmount(order1.getTotalPrice()); // SỬA: Lấy giá từ Order
        payment1.setStatus(PaymentStatus.SUCCESS);
        payment1.setTransactionRef("TXN_PAID_123");
        payment1.setOrderInfo("Thanh toán vé tàu (Data init)");
        payment1.setBankCode("VCB");
        payment1.setResponseCode("00");
        payment1.setPayDate(LocalDateTime.now().minusDays(1).plusMinutes(15));
        paymentRepository.save(payment1);

        // Cập nhật trạng thái cho Order và Booking
        order1.setStatus(PaymentStatus.SUCCESS);
        orderRepository.save(order1);

        booking1.setStatus(BookingStatus.PAID);
        bookingRepository.save(booking1);


        // --- ĐƠN HÀNG 2: (SE1, Ghế B1) - CHƯA THANH TOÁN ---
        Order order2 = bookingService.createOrder(
                customer.getId(),
                tripSE1.getTripId(),
                List.of(se1_normal_seat_B1.getSeatId()), // Phải là List
                "Người Đi Cùng",
                "ADULT", // Thêm loại hành khách
                customer.getPhone(),
                customer.getEmail()
        );
        // (Không tạo Ticket, Payment. Status mặc định là BOOKED và Order PENDING)
        // Ghế se1_normal_seat_B1 sẽ có status = BOOKED


        // --- ĐƠN HÀNG 3: (SE3, Ghế A1) - CHƯA THANH TOÁN ---
        Order order3 = bookingService.createOrder(
                customer.getId(),
                tripSE3.getTripId(),
                List.of(se3_vip_seat_A1.getSeatId()), // Phải là List
                customer.getFullName(),
                "ADULT", // Thêm loại hành khách
                customer.getPhone(),
                customer.getEmail()
        );
        // (Status mặc định là BOOKED và Order PENDING)
    }
}
