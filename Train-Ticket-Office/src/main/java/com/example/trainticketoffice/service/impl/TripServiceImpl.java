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
import lombok.RequiredArgsConstructor; // THÊM
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors; // THÊM

@Service
@RequiredArgsConstructor // THÊM: Để thay thế @Autowired
public class TripServiceImpl implements TripService {

    // SỬA: Bỏ @Autowired, thêm 'final'
    private final TripRepository tripRepository;
    private final TrainRepository trainRepository;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final SeatRepository seatRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository; // THÊM

    @Override
    public List<Trip> getAllTrips() {
        return tripRepository.findAll();
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

    // ===== SỬA HOÀN TOÀN LOGIC HÀM NÀY =====
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

        // 1. Lấy ra các Order (đơn hàng) duy nhất từ các booking
        List<Order> orders = bookings.stream()
                .map(Booking::getOrder)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // 2. Xóa Tickets
        for (Booking booking : bookings) {
            List<Ticket> tickets = ticketRepository.findByBooking(booking);
            ticketRepository.deleteAllInBatch(tickets);
        }

        // 3. Xóa Payments (liên kết với Order)
        for (Order order : orders) {
            // SỬA: Lỗi nằm ở đây, phải tìm theo Order
            List<Payment> payments = paymentRepository.findByOrder(order);
            paymentRepository.deleteAllInBatch(payments);
        }

        // 4. Xóa Bookings
        bookingRepository.deleteAllInBatch(bookings);

        // 5. Xóa Orders
        orderRepository.deleteAllInBatch(orders);

        // 6. Xóa Chuyến (Trip)
        tripRepository.delete(trip);
    }
    // ======================================

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