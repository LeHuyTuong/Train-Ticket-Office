package com.example.trainticketoffice.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp Gói Dữ Liệu (Form Model)
 * Đại diện cho toàn bộ Form Nhập Thông tin Hành khách.
 */
@Data
public class BookingRequest {

    private Long tripId;

    // Spring sẽ tự động điền danh sách này từ form
    private List<PassengerInfo> passengers = new ArrayList<>();
}