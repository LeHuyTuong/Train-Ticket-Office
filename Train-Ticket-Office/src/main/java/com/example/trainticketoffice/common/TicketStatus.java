package com.example.trainticketoffice.common;

public enum TicketStatus {
    ACTIVE("active", "Vé đang hoạt động, chờ check-in"),
    CHECKED_IN("checked_in", "Vé đã được check-in"),
    CANCELLED("cancelled", "Vé đã bị hủy"),
    EXPIRED("expired", "Vé đã hết hiệu lực"); // <-- THÊM MỚI

    private final String dbValue;
    private final String description;

    TicketStatus(String dbValue, String description) {
        this.dbValue = dbValue;
        this.description = description;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getDescription() {
        return description;
    }
}