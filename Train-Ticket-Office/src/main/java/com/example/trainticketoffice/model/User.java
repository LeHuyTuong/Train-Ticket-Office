package com.example.trainticketoffice.model;

import jakarta.persistence.*;
import lombok.*;

import javax.management.relation.Role;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Getter
@Setter
@Table(name = "Users")

public class User {
    // TODO : Quốc Bảo Repo + Service CRUD
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "email",nullable = false)
    private String email;

    @Column(name = "password",nullable = false)
    private String password;

    @Column(name = "fullName",nullable = false)
    private String fullName;

    @Column(name = "phone",nullable = false)
    private String phone;

    @Column(name = "create_Date")
    private LocalDate createDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "role",nullable = false)
    private Role role;

    public enum Role {
        STAFF,
        CUSTOMER
    }
}
