package com.example.trainticketoffice.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Getter
@Setter
@Table(name = "stations")
public class Station extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "code", unique = true, nullable = false, length = 10, columnDefinition = "nvarchar(10)") // SỬA
    private String code;

    @Column(name = "name", nullable = false, length = 100, columnDefinition = "nvarchar(100)") // SỬA
    private String name;

    @Column(name = "city", nullable = false, length = 50, columnDefinition = "nvarchar(50)") // SỬA
    private String city;

    @Column(name = "province", nullable = false, length = 50, columnDefinition = "nvarchar(50)") // SỬA
    private String province;

    @Column(name = "distance_km")
    private Integer distanceKm;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.ACTIVE;

    public enum Status {
        ACTIVE,
        INACTIVE
    }
}