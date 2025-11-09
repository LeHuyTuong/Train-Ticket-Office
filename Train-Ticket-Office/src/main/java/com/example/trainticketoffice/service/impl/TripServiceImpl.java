package com.example.trainticketoffice.service.impl;

import com.example.trainticketoffice.common.BookingStatus;
import com.example.trainticketoffice.common.TicketStatus;
import com.example.trainticketoffice.common.TrainStatus;
import com.example.trainticketoffice.common.TripStatus;
import com.example.trainticketoffice.common.SeatStatus;
import com.example.trainticketoffice.model.*;
import com.example.trainticketoffice.repository.*;
import com.example.trainticketoffice.service.TripService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page; // <-- THÊM
import org.springframework.data.domain.PageRequest; // <-- THÊM
import org.springframework.data.domain.Pageable; // <-- THÊM
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TripServiceImpl implements TripService {

    // ===== SỐ LƯỢNG CHUYẾN TRÊN MỖI TRANG =====
    public static final int TRIPS_PER_PAGE = 5;

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

    @Override
    public List<Trip> getAllTrips() {
        return tripRepository.findAll();
    }

    // ===== IMPLEMENT HÀM PHÂN TRANG MỚI =====
    @Override
    public Page<Trip> listAllAdmin(int pageNum, Integer stationId) {
        Pageable pageable = PageRequest.of(pageNum - 1, TRIPS_PER_PAGE);

        if (stationId != null) {
            // Nếu có lọc theo Ga, gọi hàm tìm kiếm
            return tripRepository.findByRoute_StartStation_IdOrRoute_EndStation_Id(stationId, stationId, pageable);
        }

        // Mặc định, trả về tất cả (đã phân trang)
        return tripRepository.findAll(pageable);
    }
    // ======================================

    @Override
    public Optional<Trip> getTripById(Long id) {
        return tripRepository.findById(id);
    }

    @Override
    @Transactional
    public Trip saveTrip(Trip trip) {
        // (Code hàm này giữ nguyên)
        if (trip.getTripId() == null) {
            Train train = trainRepository.findById(trip.getTrain().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Train ID"));

            if (train.getStatus() != TrainStatus.AVAILABLE) {
                throw new IllegalStateException("Tàu " + train.getCode() + " không rảnh (đang chạy hoặc bảo trì).");
            }

            train.setStatus(TrainStatus.ON_TRIP);
            trainRepository.save(train);
        }

        if(trip.getStatus() == null) {
            trip.setStatus(TripStatus.UPCOMING);
        }

        return tripRepository.save(trip);
    }

    @Override
    @Transactional
    public void deleteTrip(Long id) {
        // (Code hàm này giữ nguyên)
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

            // Sửa logic (nếu dùng Order)
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
        // (Code hàm này giữ nguyên)
        LocalDateTime startTime = departureDate.atStartOfDay();
        LocalDateTime endTime = departureDate.atTime(23, 59, 59);
        return tripRepository.findAllByRouteAndDepartureTimeBetween(route, startTime, endTime);
    }

    @Override
    @Transactional
    public void updateTripStatus(Long tripId, TripStatus newStatus) {
        // (Code hàm này giữ nguyên)
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
                    train.setStatus(TrainStatus.AVAILABLE);
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
                if (train != null && train.getStatus() != TrainStatus.ON_TRIP) {
                    train.setStatus(TrainStatus.ON_TRIP);
                    trainRepository.save(train);
                }
                break;
        }

        tripRepository.save(trip);
    }
}