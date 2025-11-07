package com.example.trainticketoffice.model;

import com.example.trainticketoffice.common.BookingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal; // <-- THÊM
import java.time.LocalDate;
import java.time.LocalDateTime; // <-- THÊM

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Booking extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Long bookingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id")
    private Seat seat;

    // ===== THÊM QUAN HỆ NÀY =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id") // Tên cột khóa ngoại trong bảng bookings
    private Order order;
    // =============================

    @Column(nullable = false, length = 100)
    private String passengerName;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    // ===== THÊM TRƯỜNG NÀY =====
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.BOOKED;

    // ===== SỬA TỪ LocalDate thành LocalDateTime =====
    @Column(name = "booking_time", nullable = false)
    private LocalDateTime bookingTime;
}