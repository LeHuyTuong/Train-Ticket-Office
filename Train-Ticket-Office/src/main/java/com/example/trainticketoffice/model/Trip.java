package com.example.trainticketoffice.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "trips")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Trip extends BaseEntity{
    // TODO má t lười quá , đứa nào làm hộ đi

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trip_id")
    private Long tripId;

    // Nếu Train là một đối tượng detached,(đính kèm)
    // nó sẽ được "merge" (gắn lại) vào phiên làm việc hiện tại thay vì cố gắng "persist" (lưu mới).
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
    private String arrival_station;

    @NotNull(message = "Departure time is mandatory")
    @Column(name = "departure_time")
    private LocalDate departure_time;

    @NotNull(message = "Arrival time is mandatory")
    @Column(name = "arrival_time")
    private LocalDate arrival_time;

    @Column(name = "price")
    private double price;

}
