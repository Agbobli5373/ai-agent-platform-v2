package com.platform.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_embeddings", indexes = {
        @Index(name = "idx_document_embeddings_document_id", columnList = "document_id")
})
public class DocumentEmbedding extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    public Document document;

    @Column(name = "chunk_index", nullable = false)
    public Integer chunkIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String content;

    @Column(columnDefinition = "vector(1024)")
    @org.hibernate.annotations.Type(VectorType.class)
    public float[] embedding;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    public String metadata;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
