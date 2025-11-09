package com.example.trainticketoffice.model;

import com.example.trainticketoffice.common.BookingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate; // THÊM
import java.time.LocalDateTime;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false, length = 100, columnDefinition = "nvarchar(100)")
    private String passengerName;

    @Column(length = 20, columnDefinition = "nvarchar(20)")
    private String passengerType;

    // ===== 2 TRƯỜNG MỚI (LƯU VÀO DB) =====
    @Column(length = 20, columnDefinition = "nvarchar(20)")
    private String passengerIdCard; // CCCD/Passport

    @Column(name = "date_of_birth")
    private LocalDate dob; // Ngày sinh
    // ===================================

    @Column(length = 20, columnDefinition = "nvarchar(20)")
    private String phone;

    @Column(length = 100, columnDefinition = "nvarchar(100)")
    private String email;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.BOOKED;

    @Column(name = "booking_time", nullable = false)
    private LocalDateTime bookingTime;
}