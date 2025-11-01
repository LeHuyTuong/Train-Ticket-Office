package com.example.trainticketoffice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trains")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Train {
    // TODO Hân : repo + full CRUD
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Train code is mandatory")
    @Size(min = 2, max = 10, message = "Code must be between 2 and 10 characters")
    @Column(unique = true, nullable = false)
    private String code;

    @NotBlank(message = "Train name is mandatory")
    @Column(nullable = false)
    private String name;

    @Min(value = 1, message = "There must be at least 1 carriage")
    @Column(nullable = false)
    private Integer totalCarriages;

    @Min(value = 1, message = "Seat capacity must be at least 1")
    @Column(nullable = false)
    private Integer seatCapacity;

    @NotBlank(message = "Status is mandatory")
    @Column(nullable = false)
    private String status;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @ToString.Exclude
    @OneToMany(mappedBy = "train", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Seat> seats = new ArrayList<>();
}
