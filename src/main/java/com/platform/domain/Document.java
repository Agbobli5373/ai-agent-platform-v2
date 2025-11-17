package com.platform.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documents", indexes = {
        @Index(name = "idx_documents_organization_id", columnList = "organization_id"),
        @Index(name = "idx_documents_status", columnList = "status")
})
public class Document extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(nullable = false, length = 500)
    public String filename;

    @Column(name = "content_type", nullable = false, length = 100)
    public String contentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    public Organization organization;

    @Column(name = "size_bytes", nullable = false)
    public Long sizeBytes;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    public DocumentStatus status;

    @Column(name = "uploaded_at", nullable = false)
    public LocalDateTime uploadedAt;

    @Column(name = "indexed_at")
    public LocalDateTime indexedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    public String metadata;

    @PrePersist
    public void prePersist() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = DocumentStatus.PENDING;
        }
    }

    public enum DocumentStatus {
        PENDING,
        INDEXED,
        FAILED
    }
}
