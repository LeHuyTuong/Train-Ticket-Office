package com.example.trainticketoffice.model;

import com.example.trainticketoffice.common.SeatStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "seats")
@Data
@NoArgsConstructor
@AllArgsConstructor
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
    @Column(name = "seat_number", nullable = false, columnDefinition = "nvarchar(10)")
    private String seatNumber;


    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SeatStatus status;

    private Boolean isActive = true;
}