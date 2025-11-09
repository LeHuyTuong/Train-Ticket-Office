package com.example.trainticketoffice.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Getter
@Setter
@Table(name = "routes")
public class Route extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "code", unique = true, nullable = false, length = 20, columnDefinition = "NVARCHAR(20)")
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "start_station_id", nullable = false)
    private Station startStation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "end_station_id", nullable = false)
    private Station endStation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.ACTIVE;

    public enum Status {
        ACTIVE,
        INACTIVE
    }
}
