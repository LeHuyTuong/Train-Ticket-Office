package com.example.trainticketoffice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.example.trainticketoffice.common.SeatStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
public class Seat extends BaseEntity {

    @Id
    @Column(name = "seat_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seatId;

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

}