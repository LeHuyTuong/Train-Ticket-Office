package com.example.trainticketoffice.model;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp Gói Dữ Liệu (Form Model)
 * Đại diện cho toàn bộ Form Nhập Thông tin Hành khách.
 */
@Data
// Giả sử đây là file BookingRequest.java của bạn
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {

    private Long tripId;

    private List<PassengerInfo> passengers = new ArrayList<>();

    private String context;
}
