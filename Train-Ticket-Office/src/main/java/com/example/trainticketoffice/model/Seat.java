package com.example.trainticketoffice.model;


import com.example.trainticketoffice.common.SeatStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "seats")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Seat {

    // TODO Ngoc Anh Repo + Full CRUD

    @Id
    @Column(name = "seat_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seatId;


    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SeatStatus status;


}
