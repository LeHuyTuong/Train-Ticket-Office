package com.example.trainticketoffice.model;

import com.example.trainticketoffice.common.BookingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal; // <-- THÊM
import java.time.LocalDateTime; // <-- THÊM

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
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

    // THÊM: Liên kết với Order
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false, length = 100)
    private String passengerName;

    // THÊM: Loại hành khách (ADULT, CHILD, SENIOR)
    @Column(length = 20)
    private String passengerType;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    // THÊM: Giá gốc (trước khi giảm giá)
    @Column(precision = 10, scale = 2)
    private BigDecimal originalPrice;

    // SỬA: Đổi tên từ 'price' (trong file cũ) sang 'finalPrice'
    // (File cũ của bạn là 'price', nhưng tôi đổi tên để rõ nghĩa)
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.BOOKED;

    @Column(name = "booking_time", nullable = false)
    private LocalDateTime bookingTime;
}