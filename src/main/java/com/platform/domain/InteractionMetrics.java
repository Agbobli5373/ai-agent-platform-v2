package com.platform.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "interaction_metrics")
public class InteractionMetrics extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    public Agent agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    public Conversation conversation;

    @Column(name = "response_time_ms", nullable = false)
    public Integer responseTimeMs;

    @Column(name = "token_usage", nullable = false)
    public Integer tokenUsage;

    @Column(name = "tool_calls")
    public Integer toolCalls = 0;

    @Column(nullable = false)
    public LocalDateTime timestamp;

    @Column(nullable = false)
    public Boolean success;

    @PrePersist
    public void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (toolCalls == null) {
            toolCalls = 0;
        }
    }
}
