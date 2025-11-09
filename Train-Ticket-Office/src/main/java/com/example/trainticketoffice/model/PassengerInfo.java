package com.example.trainticketoffice.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate; // THÊM

/**
 * Lớp Gói Dữ Liệu (Form Model)
 * Dùng để giữ thông tin của MỘT hành khách được nhập từ form.
 */
@Data
public class PassengerInfo {

    private Long seatId;
    private String seatNumber;
    private String seatTypeName;
    private BigDecimal basePrice;

    // 4 trường cũ
    private String passengerName;
    private String phone;
    private String email;
    private String passengerType = "ADULT";

    // ===== 2 TRƯỜNG MỚI =====
    private String passengerIdCard; // CCCD/Passport
    private LocalDate dob; // Ngày sinh
    // ========================
}