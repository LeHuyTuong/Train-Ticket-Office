package com.example.trainticketoffice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "admin_wallet")
@Data
@NoArgsConstructor
public class AdminWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    public AdminWallet(BigDecimal initialBalance) {
        this.balance = initialBalance;
    }
}