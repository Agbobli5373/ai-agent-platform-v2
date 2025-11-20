package com.platform.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "api_keys", indexes = {
        @Index(name = "idx_api_keys_key_hash", columnList = "key_hash"),
        @Index(name = "idx_api_keys_user_id", columnList = "user_id"),
        @Index(name = "idx_api_keys_organization_id", columnList = "organization_id")
})
public class ApiKey extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "key_hash", nullable = false, unique = true, length = 255)
    public String keyHash;

    @Column(nullable = false, length = 255)
    public String name;

    @Column(columnDefinition = "TEXT")
    public String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    public Organization organization;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @Column(name = "last_used_at")
    public LocalDateTime lastUsedAt;

    @Column(name = "expires_at")
    public LocalDateTime expiresAt;

    @Column(nullable = false)
    public Boolean active = true;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return active && !isExpired();
    }
}
