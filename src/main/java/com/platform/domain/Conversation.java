package com.platform.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "conversations", indexes = {
        @Index(name = "idx_conversations_agent_id", columnList = "agent_id"),
        @Index(name = "idx_conversations_user_id", columnList = "user_id"),
        @Index(name = "idx_conversations_status", columnList = "status")
})
public class Conversation extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    public Agent agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public User user;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 25)
    public List<Message> messages = new ArrayList<>();

    @Column(name = "started_at", nullable = false)
    public LocalDateTime startedAt;

    @Column(name = "ended_at")
    public LocalDateTime endedAt;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    public ConversationStatus status;

    @Column(name = "satisfaction_score")
    public Integer satisfactionScore;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (startedAt == null) {
            startedAt = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (status == null) {
            status = ConversationStatus.ACTIVE;
        }
    }

    public enum ConversationStatus {
        ACTIVE,
        ENDED,
        ARCHIVED
    }
}
