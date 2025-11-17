package com.platform.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tools", indexes = {
        @Index(name = "idx_tools_owner_id", columnList = "owner_id"),
        @Index(name = "idx_tools_type", columnList = "type")
})
public class Tool extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(nullable = false, length = 255)
    public String name;

    @Column(columnDefinition = "TEXT")
    public String description;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    public ToolType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    public User owner;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String endpoint;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "auth_config")
    public String authConfig;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parameters")
    public String parameters;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public enum ToolType {
        REST_API,
        FUNCTION,
        DATABASE
    }
}
