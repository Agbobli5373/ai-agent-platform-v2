package com.platform.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "organizations")
public class Organization extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(nullable = false, length = 255)
    public String name;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @Column(name = "usage_limit", columnDefinition = "jsonb")
    public String usageLimit;

    @Column(columnDefinition = "jsonb")
    public String settings;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
