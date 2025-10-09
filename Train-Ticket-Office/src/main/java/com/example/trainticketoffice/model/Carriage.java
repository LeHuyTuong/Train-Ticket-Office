package com.example.trainticketoffice.model;

import com.example.trainticketoffice.common.TypeOfCarriage;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "carriages")
@NoArgsConstructor
@Data
@AllArgsConstructor
public class Carriage {
    // TODO T Bao - repo + CRUD
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "carriage_id")
    private int carriageId;

    @ManyToOne
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    @Column(name = "code",  nullable = false)
    private String code;  // toa 1 toa 2

    @Enumerated(EnumType.STRING) // Thêm @Enumerated để lưu tên của Enum (CHAIR, BED) thay vì số (0, 1)
    @Column(name = "type" , nullable = false)
    private TypeOfCarriage type ;

}
