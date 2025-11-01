package com.example.trainticketoffice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Booking extends BaseEntity{

    // TODO Tuong luoi lam vai cut

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Long bookingId;

    // Ai đặt vé
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    // Chuyến (nếu dùng Schedule thì đổi Trip -> Schedule + cột trip_id -> schedule_id)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    // Ghế
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id")
    private Seat seat;

    // Thông tin hành khách (1 booking = 1 ghế)
    @Column(nullable = false, length = 100)
    private String passengerName;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    // BOOKED / PAID / CANCELLED (để String cho dễ)
    @Column(nullable = false, length = 20)
    private String status = "BOOKED";

    @Column(name = "booking_time", nullable = false)
    private LocalDate bookingTime;


}
