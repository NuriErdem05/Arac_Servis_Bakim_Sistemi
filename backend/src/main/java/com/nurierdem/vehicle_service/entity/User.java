package com.nurierdem.vehicle_service.entity;

import com.nurierdem.vehicle_service.enums.UserRole;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 16)
    private UserRole role = UserRole.USER;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Column(name = "credential_version", nullable = false)
    private long credentialVersion;

    @Column(nullable = false)
    private boolean archived;

    public boolean hasPassword() {
        return passwordHash != null && !passwordHash.isBlank();
    }

    public User() {
    }
}
