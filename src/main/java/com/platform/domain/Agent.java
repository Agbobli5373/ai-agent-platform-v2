package com.platform.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "agents", indexes = {
        @Index(name = "idx_agents_organization_id", columnList = "organization_id"),
        @Index(name = "idx_agents_owner_id", columnList = "owner_id"),
        @Index(name = "idx_agents_status", columnList = "status")
})
public class Agent extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(nullable = false, length = 255)
    public String name;

    @Column(columnDefinition = "TEXT")
    public String description;

    @Column(name = "system_prompt", nullable = false, columnDefinition = "TEXT")
    public String systemPrompt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    public User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    public Organization organization;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    public AgentStatus status;

    @Column(name = "model_name", nullable = false, length = 100)
    public String modelName;

    @OneToMany(mappedBy = "agent", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 25)
    public List<AgentTool> tools = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration")
    public String configuration;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum AgentStatus {
        ACTIVE,
        INACTIVE,
        PAUSED,
        DELETED
    }
}
