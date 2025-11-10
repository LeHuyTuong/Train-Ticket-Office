package com.example.trainticketoffice.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList; // THÊM
import java.util.List; // THÊM

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    @Column(name = "name", nullable = false, columnDefinition = "nvarchar(255)")
    private String name;

    @Column(name = "type", nullable = false, columnDefinition = "nvarchar(255)")
    private String type;

    @Column(name = "position")
    private int position;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "seat_type_id")
    private SeatType seatType;


    @OneToMany(mappedBy = "carriage", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Seat> seats = new ArrayList<>();

    @Transient
    public Integer getCapacity() {
        return this.seats != null ? this.seats.size() : 0;
    }
}