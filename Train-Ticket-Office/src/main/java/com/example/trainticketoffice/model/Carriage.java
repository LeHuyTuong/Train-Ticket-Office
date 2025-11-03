package com.example.trainticketoffice.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carriages")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Carriage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "carriage_id")
    private Long carriageId;

    // Toa này thuộc tàu nào
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    // Ví dụ: "Toa 1", "Toa 2"
    @Column(name = "name", nullable = false)
    private String name;

    // Ví dụ: "Ngồi mềm điều hòa", "Giường nằm"
    @Column(name = "type", nullable = false)
    private String type;

    // Thứ tự sắp xếp
    @Column(name = "position")
    private int position;

    // Một toa có nhiều ghế
    @OneToMany(mappedBy = "carriage", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Seat> seats = new ArrayList<>();
}