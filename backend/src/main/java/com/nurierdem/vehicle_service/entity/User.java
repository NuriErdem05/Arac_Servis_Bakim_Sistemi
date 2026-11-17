package com.nurierdem.vehicle_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length =150)
    private String fullName;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    public User() {
    }
}
