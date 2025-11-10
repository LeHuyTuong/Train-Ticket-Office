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
    private String passengerName;
    private String phone;
    private String email;
    private String passengerType = "ADULT";
    private String passengerIdCard;
    private LocalDate dob;
}