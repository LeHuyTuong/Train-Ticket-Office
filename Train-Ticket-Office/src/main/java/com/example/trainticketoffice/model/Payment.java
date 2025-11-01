package com.example.trainticketoffice.model;


import jakarta.persistence.*;

import java.time.LocalDateTime;

public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private double amount;

    @Column(nullable = false)
    private LocalDateTime date = LocalDateTime.now();

}
