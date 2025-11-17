package com.platform.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(nullable = false, unique = true, length = 255)
    public String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    public String passwordHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    public Organization organization;

    @Column(nullable = false, length = 50)
    public String role;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @Column(name = "last_login")
    public LocalDateTime lastLogin;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        // Normalize email to lowercase for consistency
        if (email != null) {
            email = email.trim().toLowerCase();
        }
    }

    @PreUpdate
    public void preUpdate() {
        // Normalize email to lowercase for consistency
        if (email != null) {
            email = email.trim().toLowerCase();
        }
    }
}
