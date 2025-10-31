package com.example.trainticketoffice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trains")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Train {
    // TODO Hân : repo + full CRUD

    @Id
    @Column(name = "train_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int trainId;

    @Column(name = "name")
    private String name ; // SE1 TN2

    @Column(name = "route")
    private String route;

}
