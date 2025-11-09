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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TripServiceImpl implements TripService {

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
        // ===== SỬA LOGIC KIỂM TRA TÀU =====
        if (trip.getTripId() == null) {
            Train train = trainRepository.findById(trip.getTrain().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Train ID"));

            // CHỈ KIỂM TRA BẢO TRÌ
            if (train.getStatus() == TrainStatus.MAINTENANCE) {
                throw new IllegalStateException("Tàu " + train.getCode() + " đang bảo trì.");
            }

            // KHÔNG ĐẶT ON_TRIP NỮA. Tàu vẫn AVAILABLE cho đến khi chạy.
            // train.setStatus(TrainStatus.ON_TRIP);
            // trainRepository.save(train);
        }

        if(trip.getStatus() == null) {
            trip.setStatus(TripStatus.UPCOMING);
        }

        return tripRepository.save(trip);
    }
    // ===================================

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
                    train.setStatus(TrainStatus.AVAILABLE); // Tàu rảnh
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
                    seat.setStatus(SeatStatus.AVAILABLE); // Trả ghế
                    seatRepository.save(seat);
                }
                break;

            case DELAYED:
                trip.setStatus(TripStatus.DELAYED);
                break;

            case IN_PROGRESS:
                trip.setStatus(TripStatus.IN_PROGRESS);
                if (train != null && train.getStatus() != TrainStatus.ON_TRIP) {
                    train.setStatus(TrainStatus.ON_TRIP); // Tàu bận
                    trainRepository.save(train);
                }
                break;

            // ===== SỬA LOGIC NÀY =====
            case UPCOMING:
                trip.setStatus(TripStatus.UPCOMING);
                // KHÔNG set Tàu thành ON_TRIP khi chuyến đi SẮP DIỄN RA
                // (Tàu vẫn AVAILABLE)
                break;
            // =========================
        }

        tripRepository.save(trip);
    }
}