package com.example.trainticketoffice.model;
import com.example.trainticketoffice.common.TrainStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name = "trains")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class Train {
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
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrainStatus status = TrainStatus.AVAILABLE;
    @OneToMany(mappedBy = "train", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Carriage> carriages = new ArrayList<>();
}