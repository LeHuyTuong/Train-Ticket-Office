package com.example.trainticketoffice.service.impl;

import com.example.trainticketoffice.common.BookingStatus;
import com.example.trainticketoffice.common.TicketStatus;
import com.example.trainticketoffice.common.TrainStatus;
import com.example.trainticketoffice.common.TripStatus;
import com.example.trainticketoffice.common.SeatStatus;
import com.example.trainticketoffice.model.*;
import com.example.trainticketoffice.repository.*;
import com.example.trainticketoffice.service.StationService; // <-- THÊM
import com.example.trainticketoffice.service.RouteService;   // <-- THÊM
import com.example.trainticketoffice.service.TripService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal; // <-- THÊM
import java.math.RoundingMode; // <-- THÊM
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month; // <-- THÊM
import java.util.HashMap; // <-- THÊM
import java.util.List;
import java.util.Map; // <-- THÊM
import java.util.Optional;
import java.util.stream.Collectors; // <-- THÊM

@Service
public class TripServiceImpl implements TripService {

    public static final int TRIPS_PER_PAGE = 5;

    // Các repo đã có
    @Autowired
    private TripRepository tripRepository;
    @Autowired
    private TrainRepository trainRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private SeatRepository seatRepository;
    @Autowired
    private PaymentRepository paymentRepository;

    // ===== CÁC REPO/SERVICE MỚI CẦN CHO LOGIC TÍNH TOÁN =====
    @Autowired
    private StationRepository stationRepository; // Dùng StationRepository thay vì StationService
    @Autowired
    private RouteRepository routeRepository;     // Dùng RouteRepository thay vì RouteService

    // Biến hằng số cho phụ thu
    private static final BigDecimal HOLIDAY_SURCHARGE_RATE = new BigDecimal("1.20");

    // ===== HÀM LOGIC ĐƯỢC CHUYỂN VÀO TỪ CONTROLLER =====

    /**
     * Hàm private kiểm tra ngày lễ (chuyển từ Controller vào)
     */
    private boolean isHoliday(LocalDate date) {
        if (date.getMonth() == Month.JANUARY && date.getDayOfMonth() == 1) return true;
        if (date.getMonth() == Month.APRIL && date.getDayOfMonth() == 30) return true;
        if (date.getMonth() == Month.MAY && date.getDayOfMonth() == 1) return true;
        if (date.getMonth() == Month.SEPTEMBER && date.getDayOfMonth() == 2) return true;
        return false;
    }

    /**
     * Logic tìm kiếm chuyến đi, tính giá, đếm vé
     * (Đã ở trong Service Layer)
     */
    @Override
    public Map<String, Object> findOneWayTrips(Integer startId, Integer endId, LocalDate date) {
        Map<String, Object> modelData = new HashMap<>();

        // Dùng repo theo convention của file này
        List<Route> routeOpt = routeRepository.findByStartStationIdAndEndStationId(startId, endId);
        Optional<Station> startStationOpt = stationRepository.findById(startId);
        Optional<Station> endStationOpt = stationRepository.findById(endId);

        if (routeOpt.isEmpty() || startStationOpt.isEmpty() || endStationOpt.isEmpty()) {
            modelData.put("errorMessage", "Không tìm thấy tuyến đường nào phù hợp.");
            return modelData;
        }

        Station startStation = startStationOpt.get();
        Station endStation = endStationOpt.get();

        //Lấy danh sách chuyến đi
        List<Trip> availableTrips;
        if (date != null) {
            availableTrips = tripRepository.findAllByRouteAndDepartureTimeBetween(routeOpt.get(0), date.atStartOfDay(), date.atTime(23, 59, 59));
        } else {
            availableTrips = tripRepository.findAllByRoute(routeOpt.get(0));
        }

        // Lấy KM
        if (startStation.getDistanceKm() == null || endStation.getDistanceKm() == null) {
            modelData.put("errorMessage", "Lỗi cấu hình: Ga chưa có thông tin KM.");
            return modelData;
        }
        int distanceKm = Math.abs(endStation.getDistanceKm() - startStation.getDistanceKm());
        if (distanceKm == 0) distanceKm = 20;


        Map<Long, Long> availableVipCounts = new HashMap<>();
        Map<Long, Long> availableNormalCounts = new HashMap<>();
        Map<Long, BigDecimal> tripMinPrices = new HashMap<>();

        for (Trip trip : availableTrips) {
            // Kiểm tra Lễ
            boolean isTripOnHoliday = isHoliday(trip.getDepartureTime().toLocalDate());
            BigDecimal currentSurchargeRate = isTripOnHoliday ? HOLIDAY_SURCHARGE_RATE : BigDecimal.ONE;

            //Lấy các Seat ID đã bị đặt
            List<Long> bookedSeatIds = bookingRepository.findAllByTrip_TripIdAndStatusIn(
                            trip.getTripId(),
                            List.of(BookingStatus.BOOKED, BookingStatus.PAID, BookingStatus.COMPLETED)
                    ).stream()
                    .map(booking -> booking.getSeat().getSeatId())
                    .collect(Collectors.toList());

            long vipCount = 0;
            long normalCount = 0;
            BigDecimal minPriceInTrip = null;

            //Lặp qua các Toa -> Ghế để đếm và tính giá
            Train train = trip.getTrain();
            for (Carriage carriage : train.getCarriages()) {
                SeatType seatType = carriage.getSeatType();
                if (seatType == null || seatType.getPricePerKm() == null) continue;

                BigDecimal basePrice = seatType.getPricePerKm().multiply(BigDecimal.valueOf(distanceKm));
                BigDecimal priceWithSurcharge = basePrice.multiply(currentSurchargeRate);
                BigDecimal finalPrice = priceWithSurcharge.setScale(0, RoundingMode.HALF_UP);

                if (minPriceInTrip == null || finalPrice.compareTo(minPriceInTrip) < 0) {
                    minPriceInTrip = finalPrice;
                }

                for (Seat seat : carriage.getSeats()) {
                    if (!bookedSeatIds.contains(seat.getSeatId())) {
                        if (seatType.getName().toLowerCase().contains("vip")) {
                            vipCount++;
                        } else {
                            normalCount++;
                        }
                    }
                }
            }
            availableVipCounts.put(trip.getTripId(), vipCount);
            availableNormalCounts.put(trip.getTripId(), normalCount);
            tripMinPrices.put(trip.getTripId(), minPriceInTrip != null ? minPriceInTrip : BigDecimal.ZERO);
        }
        modelData.put("trips", availableTrips);
        modelData.put("vipCounts", availableVipCounts);
        modelData.put("normalCounts", availableNormalCounts);
        modelData.put("minPrices", tripMinPrices);
        modelData.put("startStation", startStation);
        modelData.put("endStation", endStation);

        return modelData;
    }

    /**
     * Logic lấy tất cả chuyến sắp đi, tính giá, đếm vé
     * (Đã ở trong Service Layer)
     */
    @Override
    public Map<String, Object> getAllAvailableTripsForDisplay() {
        Map<String, Object> modelData = new HashMap<>();

        List<Trip> allTrips = tripRepository.findAll().stream()
                .filter(trip -> trip.getStatus() == TripStatus.UPCOMING || trip.getStatus() == TripStatus.DELAYED)
                .collect(Collectors.toList());

        Map<Long, Long> availableVipCounts = new HashMap<>();
        Map<Long, Long> availableNormalCounts = new HashMap<>();
        Map<Long, BigDecimal> tripMinPrices = new HashMap<>();

        for (Trip trip : allTrips) {
            Station startStation = trip.getRoute().getStartStation();
            Station endStation = trip.getRoute().getEndStation();

            if (startStation.getDistanceKm() == null || endStation.getDistanceKm() == null) continue;
            int distanceKm = Math.abs(endStation.getDistanceKm() - startStation.getDistanceKm());
            if (distanceKm == 0) distanceKm = 20;

            boolean isTripOnHoliday = isHoliday(trip.getDepartureTime().toLocalDate());
            BigDecimal currentSurchargeRate = isTripOnHoliday ? HOLIDAY_SURCHARGE_RATE : BigDecimal.ONE;

            List<Long> bookedSeatIds = bookingRepository.findAllByTrip_TripIdAndStatusIn(
                            trip.getTripId(),
                            List.of(BookingStatus.BOOKED, BookingStatus.PAID, BookingStatus.COMPLETED)
                    ).stream()
                    .map(booking -> booking.getSeat().getSeatId())
                    .collect(Collectors.toList());

            long vipCount = 0;
            long normalCount = 0;
            BigDecimal minPriceInTrip = null;
            Train train = trip.getTrain();

            for (Carriage carriage : train.getCarriages()) {
                SeatType seatType = carriage.getSeatType();
                if (seatType == null || seatType.getPricePerKm() == null) continue;

                BigDecimal basePrice = seatType.getPricePerKm().multiply(BigDecimal.valueOf(distanceKm));
                BigDecimal priceWithSurcharge = basePrice.multiply(currentSurchargeRate);
                BigDecimal finalPrice = priceWithSurcharge.setScale(0, RoundingMode.HALF_UP);

                if (minPriceInTrip == null || finalPrice.compareTo(minPriceInTrip) < 0) {
                    minPriceInTrip = finalPrice;
                }

                for (Seat seat : carriage.getSeats()) {
                    if (!bookedSeatIds.contains(seat.getSeatId())) {
                        if (seatType.getName().toLowerCase().contains("vip")) {
                            vipCount++;
                        } else {
                            normalCount++;
                        }
                    }
                }
            }
            availableVipCounts.put(trip.getTripId(), vipCount);
            availableNormalCounts.put(trip.getTripId(), normalCount);
            tripMinPrices.put(trip.getTripId(), minPriceInTrip != null ? minPriceInTrip : BigDecimal.ZERO);
        }

        modelData.put("availableTrips", allTrips);
        modelData.put("availableVipCounts", availableVipCounts);
        modelData.put("availableNormalCounts", availableNormalCounts);
        modelData.put("tripMinPrices", tripMinPrices);

        return modelData;
    }


    // ===== CÁC HÀM GỐC (GIỮ NGUYÊN) =====

    @Override
    public List<Trip> getAllTrips() {
        return tripRepository.findAll();
    }

    @Override
    public Page<Trip> listAllAdmin(int pageNum, Integer stationId) {
        Pageable pageable = PageRequest.of(pageNum - 1, TRIPS_PER_PAGE);

        if (stationId != null) {
            return tripRepository.findByRoute_StartStation_IdOrRoute_EndStation_Id(stationId, stationId, pageable);
        }
        return tripRepository.findAll(pageable);
    }

    @Override
    public Optional<Trip> getTripById(Long id) {
        return tripRepository.findById(id);
    }

    @Override
    @Transactional
    public Trip saveTrip(Trip trip) {
        if (trip.getTripId() == null) {
            Train train = trainRepository.findById(trip.getTrain().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Train ID"));

            // CHỈ KIỂM TRA BẢO TRÌ
            if (train.getStatus() == TrainStatus.MAINTENANCE) {
                throw new IllegalStateException("Tàu " + train.getCode() + " đang bảo trì.");
            }
        }

        if(trip.getStatus() == null) {
            trip.setStatus(TripStatus.UPCOMING);
        }

        return tripRepository.save(trip);
    }


    @Override
    @Transactional
    public void deleteTrip(Long id) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Chuyến (Trip) ID: " + id));

        if (trip.getStatus() != TripStatus.COMPLETED) {
            throw new IllegalStateException("Chỉ có thể xóa chuyến đã ở trạng thái 'Hoàn thành'.");
        }

        List<Booking> bookings = bookingRepository.findAllByTrip_TripIdAndStatusIn(
                id, List.of(BookingStatus.BOOKED, BookingStatus.PAID, BookingStatus.CANCELLED, BookingStatus.COMPLETED)
        );

        for (Booking booking : bookings) {
            List<Ticket> tickets = ticketRepository.findByBooking(booking);
            ticketRepository.deleteAll(tickets);

            if(booking.getOrder() != null) {
                List<Payment> payments = paymentRepository.findByOrder(booking.getOrder());
                paymentRepository.deleteAll(payments);
            }
        }

        bookingRepository.deleteAll(bookings);
        tripRepository.delete(trip);
    }

    @Override
    public List<Trip> findTripsByRoute(Route route) {
        return tripRepository.findAllByRoute(route);
    }

    @Override
    public List<Trip> findTripsByRouteAndDate(Route route, LocalDate departureDate) {
        LocalDateTime startTime = departureDate.atStartOfDay();
        LocalDateTime endTime = departureDate.atTime(23, 59, 59);
        return tripRepository.findAllByRouteAndDepartureTimeBetween(route, startTime, endTime);
    }

    @Override
    @Transactional
    public void updateTripStatus(Long tripId, TripStatus newStatus) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Chuyến (Trip) ID: " + tripId));

        Train train = trip.getTrain();

        List<Booking> bookings = bookingRepository.findAllByTrip_TripIdAndStatusIn(
                tripId, List.of(BookingStatus.BOOKED, BookingStatus.PAID, BookingStatus.CANCELLED)
        );

        switch (newStatus) {
            case COMPLETED:
                trip.setStatus(TripStatus.COMPLETED);
                if (train != null) {
                    train.setStatus(TrainStatus.AVAILABLE);
                    trainRepository.save(train);
                }
                for (Booking booking : bookings) {
                    if(booking.getStatus() == BookingStatus.PAID || booking.getStatus() == BookingStatus.BOOKED) {
                        booking.setStatus(BookingStatus.COMPLETED);
                        bookingRepository.save(booking);
                        List<Ticket> tickets = ticketRepository.findByBooking(booking);
                        for (Ticket ticket : tickets) {
                            ticket.setStatus(TicketStatus.EXPIRED);
                            ticketRepository.save(ticket);
                        }
                    }
                }
                break;

            case CANCELLED:
                trip.setStatus(TripStatus.CANCELLED);
                if (train != null) {
                    train.setStatus(TrainStatus.AVAILABLE); // Tàu rảnh
                    trainRepository.save(train);
                }
                for (Booking booking : bookings) {
                    booking.setStatus(BookingStatus.CANCELLED);
                    bookingRepository.save(booking);
                    List<Ticket> tickets = ticketRepository.findByBooking(booking);
                    for (Ticket ticket : tickets) {
                        ticket.setStatus(TicketStatus.CANCELLED);
                        ticketRepository.save(ticket);
                    }
                    Seat seat = booking.getSeat();
                    seat.setStatus(SeatStatus.AVAILABLE);
                    seatRepository.save(seat);
                }
                break;
            case DELAYED:
                trip.setStatus(TripStatus.DELAYED);
                break;
            case IN_PROGRESS:
                trip.setStatus(TripStatus.IN_PROGRESS);
                if (train != null && train.getStatus() != TrainStatus.ON_TRIP) {
                    train.setStatus(TrainStatus.ON_TRIP);
                    trainRepository.save(train);
                }
                break;
            case UPCOMING:
                trip.setStatus(TripStatus.UPCOMING);
                break;
        }

        tripRepository.save(trip);
    }
}
