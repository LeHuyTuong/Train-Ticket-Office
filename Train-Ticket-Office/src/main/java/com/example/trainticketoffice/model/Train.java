package com.example.trainticketoffice.model;

import com.example.trainticketoffice.common.TrainStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Train code is mandatory")
    @Size(min = 2, max = 10, message = "Code must be between 2 and 10 characters")
    @Column(unique = true, nullable = false, length = 10, columnDefinition = "nvarchar(10)")
    private String code;

    @NotBlank(message = "Train name is mandatory")
    @Column(nullable = false, columnDefinition = "nvarchar(255)")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrainStatus status = TrainStatus.AVAILABLE;

    @OneToMany(mappedBy = "train", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @ToString.Exclude
    private List<Carriage> carriages = new ArrayList<>();

    @Transient
    public int getTotalCapacity() {
        if (this.carriages == null) {
            return 0;
        }
        return this.carriages.stream()
                .mapToInt(Carriage::getCapacity)
                .sum();
    }
}