package com.platform.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "messages")
public class Message extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    public Conversation conversation;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    public MessageRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String content;

    @Column(nullable = false)
    public LocalDateTime timestamp;

    @Column(name = "tool_executions", columnDefinition = "jsonb")
    public String toolExecutions;

    @Column(name = "token_count")
    public Integer tokenCount;

    @PrePersist
    public void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    public enum MessageRole {
        USER,
        ASSISTANT,
        SYSTEM
    }
}
