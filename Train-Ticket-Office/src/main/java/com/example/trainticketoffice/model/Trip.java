package com.example.trainticketoffice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

// THÊM IMPORT NÀY
import java.time.LocalDateTime;

@Entity
@Table(name = "trips")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Trip  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trip_id")
    private Long tripId;

    @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.REFRESH})
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Train train;

    @NotNull(message = "Route must be selected")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @Column(name = "departure_station")
    private String departureStation;

    @Column(name = "arrival_station")
    private String arrivalStation;

    @NotNull(message = "Departure time is mandatory")
    @Column(name = "departure_time")
    private LocalDateTime departureTime; // <-- ĐÃ SỬA TỪ LocalDate

    @NotNull(message = "Arrival time is mandatory")
    @Column(name = "arrival_time")
    private LocalDateTime arrivalTime; // <-- ĐÃ SỬA TỪ LocalDate

    @Column(name = "price")
    private double price; // Đây là giá cơ bản (ghế normal)

}