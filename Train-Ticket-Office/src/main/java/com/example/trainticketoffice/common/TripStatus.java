package com.example.trainticketoffice.common;

import lombok.Getter;

/**
 * Quản lý trạng thái vòng đời của một Chuyến đi (Trip)
 */
@Getter
public enum TripStatus {
    UPCOMING("Sắp diễn ra"),
    IN_PROGRESS("Đang chạy"),
    DELAYED("Bị hoãn"), // <-- THÊM MỚI
    COMPLETED("Đã hoàn thành"),
    CANCELLED("Đã hủy");

    private final String description;

    TripStatus(String description) {
        this.description = description;
    }

}