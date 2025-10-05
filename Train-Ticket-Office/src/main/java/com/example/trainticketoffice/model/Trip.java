package com.example.trainticketoffice.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "trips")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Trip {
    // TODO má t lười quá , đứa nào làm hộ đi

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trip_id")
    private Long tripId;

    @ManyToOne(cascade = CascadeType.ALL)
    private Train train;

    @Column(name = "departure_station")
    private String departureStation;

    @Column(name = "arrival_station")
    private String arrival_station;

    @Column(name = "departure_time")
    private LocalDate departure_time;

    @Column(name = "arrival_time")
    private LocalDate arrival_time;

    @Column(name = "price")
    private double price;

}
