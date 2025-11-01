// com/example/trainticketoffice/model/Seat.java
package com.example.trainticketoffice.model;

import com.example.trainticketoffice.common.SeatStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "seats")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true) // Cần thiết nếu BaseEntity có fields
public class Seat extends BaseEntity {

    @Id
    @Column(name = "seat_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seatId;

    // THIẾU: Mối quan hệ với Train (gây lỗi getTrain() và getTrain().getCode())
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    // THIẾU: Số ghế (gây lỗi getSeatNumber())
    @NotBlank(message = "Seat number is mandatory")
    @Column(name = "seat_number", nullable = false)
    private String seatNumber;

    @NotBlank(message = "Seat type is mandatory")
    @Column(nullable = false)
    private String seatType; // "normal", "vip"


    @NotNull(message = "Price per km is mandatory")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Column(nullable = false)
    private Double pricePerKm;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SeatStatus status;

    private Boolean isActive = true;

    // Lưu ý: Đảm bảo class Train cũng tồn tại trong package model.
}