package com.example.trainticketoffice.model;

import com.example.trainticketoffice.common.TripStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "trips")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Trip {

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

    @Column(name = "departure_station", columnDefinition = "NVARCHAR(255)")
    private String departureStation;

    @Column(name = "arrival_station", columnDefinition = "NVARCHAR(255)")
    private String arrivalStation;

    @NotNull(message = "Departure time is mandatory")
    @Column(name = "departure_time")
    private LocalDateTime departureTime;

    @NotNull(message = "Arrival time is mandatory")
    @Column(name = "arrival_time")
    private LocalDateTime arrivalTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TripStatus status = TripStatus.UPCOMING;
}
