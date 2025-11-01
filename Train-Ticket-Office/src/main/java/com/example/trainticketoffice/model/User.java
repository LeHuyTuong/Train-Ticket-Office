package com.example.trainticketoffice.model;

import com.example.trainticketoffice.common.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User extends BaseEntity{
    // TODO : Quốc Bảo Repo + Service CRUD
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private int userId;

    @Column(name = "name" , nullable = false)
    private String name;

    @Column(name = "email" , nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false, length =  50)
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;
}
