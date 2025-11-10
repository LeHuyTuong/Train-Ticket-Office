package com.example.trainticketoffice.model;

import lombok.*;

import java.time.LocalDate;

/**
 * DTO để lưu thông tin chặng về (return leg) trong Session.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RoundTripInfo {
    // Getters
    private Integer startStationId; // Ga đi (của chặng về)
    private Integer endStationId;   // Ga đến (của chặng về)
    private LocalDate departureDate; // Ngày đi (của chặng về)

}
