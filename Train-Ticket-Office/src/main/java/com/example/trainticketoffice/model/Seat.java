package com.example.trainticketoffice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "seats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Seat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Train must be selected")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id", nullable = false)
    @JsonIgnore
    private Train train;

    @NotNull(message = "Carriage number is mandatory")
    @Min(value = 1, message = "Carriage number must be at least 1")
    @Column(nullable = false)
    private Integer carriageNumber;

    @NotBlank(message = "Seat number is mandatory")
    @Column(nullable = false)
    private String seatNumber;

    @NotBlank(message = "Seat type is mandatory")
    @Column(nullable = false)
    private String seatType; // "normal", "vip"


    @NotNull(message = "Price per km is mandatory")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Column(nullable = false)
    private Double pricePerKm;

    private Boolean isActive = true;
}