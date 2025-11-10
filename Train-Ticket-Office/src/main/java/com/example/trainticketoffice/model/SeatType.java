package com.example.trainticketoffice.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "seat_types")
public class SeatType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, columnDefinition = "nvarchar(255)") // SỬA
    private String name; // Ví dụ: "Ngồi mềm điều hòa", "Giường nằm VIP 4 chỗ"

    @Column(nullable = false)
    private BigDecimal pricePerKm; // Đơn giá (VND) cho mỗi KM
}