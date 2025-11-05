package com.example.trainticketoffice.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Getter
@Setter
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "email", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String email;

    @Column(name = "password", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String password;

    @Column(name = "full_name", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String fullName;

    @Column(name = "phone", nullable = false, columnDefinition = "NVARCHAR(20)")
    private String phone;

    @Column(name = "create_date")
    private LocalDate createDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    public enum Role {
        STAFF,
        CUSTOMER
    }
}
