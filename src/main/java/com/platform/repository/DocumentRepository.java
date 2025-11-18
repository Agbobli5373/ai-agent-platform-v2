package com.platform.repository;

import com.platform.domain.Document;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class DocumentRepository implements PanacheRepositoryBase<Document, UUID> {

    public List<Document> findByOrganization(UUID organizationId) {
        return list("organization.id", organizationId);
    }

    public List<Document> findByStatus(UUID organizationId, Document.DocumentStatus status) {
        return list("organization.id = ?1 and status = ?2", organizationId, status);
    }

    public List<Document> findByContentType(UUID organizationId, String contentType) {
        return list("organization.id = ?1 and contentType = ?2", organizationId, contentType);
    }
}
