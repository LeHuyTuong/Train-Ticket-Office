package com.example.trainticketoffice.model;
import com.example.trainticketoffice.common.SeatStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
@Entity
@Table(name = "seats")
@Data @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Seat extends BaseEntity {
    @Id
    @Column(name = "seat_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seatId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carriage_id", nullable = false)
    private Carriage carriage;
    @NotBlank(message = "Seat number is mandatory")
    @Column(name = "seat_number", nullable = false)
    private String seatNumber;
    @NotBlank(message = "Seat type is mandatory")
    @Column(nullable = false)
    private String seatType;
    @NotNull(message = "Price per km is mandatory")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerKm;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SeatStatus status;
    private Boolean isActive = true;
}