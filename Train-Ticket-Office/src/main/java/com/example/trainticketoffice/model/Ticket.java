package com.example.trainticketoffice.model;

import com.example.trainticketoffice.common.TicketStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@Data
@EqualsAndHashCode(callSuper = true)
public class Ticket extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, columnDefinition = "nvarchar(255)")
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    // ===== THÊM TRƯỜNG NÀY TRỞ LẠI =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id")
    private Seat seat;
    // ===================================

    // ===== XÓA 2 TRƯỜNG NÀY =====
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "carriage_id", nullable = false)
    // private Carriage carriage;
    // @Column(name = "seat_number", length = 30, columnDefinition = "nvarchar(30)")
    // private String seatNumber;
    // =============================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_station_id", nullable = false)
    private Station fromStation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_station_id", nullable = false)
    private Station toStation;

    @Column(columnDefinition = "nvarchar(255)")
    private String passengerName;
    @Column(columnDefinition = "nvarchar(50)")
    private String passengerPhone;
    @Column(columnDefinition = "nvarchar(50)")
    private String passengerIdCard;

    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;
    private LocalDateTime bookedAt;

    private LocalDateTime checkedInAt;
}