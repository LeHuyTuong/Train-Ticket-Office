package com.example.trainticketoffice.model;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carriages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Carriage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "carriage_id")
    private Long carriageId;

    // ===== TOA NÀY THUỘC TÀU NÀO =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore // tránh vòng lặp khi serialize JSON
    private Train train;

    // ===== TÊN TOA =====
    @Column(name = "name", nullable = false, length = 100, columnDefinition = "NVARCHAR(100)")
    private String name;

    // ===== LOẠI TOA =====
    @Column(name = "type", nullable = false, length = 100, columnDefinition = "NVARCHAR(100)")
    private String type;

    // ===== THỨ TỰ SẮP XẾP =====
    @Column(name = "position")
    private int position;

    // ===== MỘT TOA CÓ NHIỀU GHẾ =====
    @OneToMany(mappedBy = "carriage", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Seat> seats = new ArrayList<>();
}
