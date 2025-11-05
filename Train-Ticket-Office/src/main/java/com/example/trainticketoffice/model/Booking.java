package com.example.trainticketoffice.model;

import com.example.trainticketoffice.common.BookingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Booking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Long bookingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    // ===== CÁC TRƯỜNG CHỮ - DÙNG NVARCHAR ĐỂ LƯU UNICODE =====
    @Column(name = "passenger_name", nullable = false, length = 100, columnDefinition = "NVARCHAR(100)")
    private String passengerName;

    @Column(name = "phone", length = 20, columnDefinition = "NVARCHAR(20)")
    private String phone;

    @Column(name = "email", length = 100, columnDefinition = "NVARCHAR(100)")
    private String email;

    // ===== GIÁ TIỀN =====
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // ===== TRẠNG THÁI ĐẶT VÉ =====
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.BOOKED;

    // ===== THỜI GIAN ĐẶT =====
    @Column(name = "booking_time", nullable = false)
    private LocalDateTime bookingTime;
}
