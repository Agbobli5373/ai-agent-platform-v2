package com.platform.repository;

import com.platform.domain.DocumentEmbedding;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class DocumentEmbeddingRepository implements PanacheRepositoryBase<DocumentEmbedding, UUID> {

    @Inject
    EntityManager em;

    public List<DocumentEmbedding> findSimilar(float[] queryEmbedding, UUID organizationId, int limit) {
        String vectorString = arrayToVectorString(queryEmbedding);

        String query = """
                SELECT e FROM DocumentEmbedding e
                WHERE e.document.organization.id = :organizationId
                ORDER BY e.embedding <=> CAST(:queryVector AS vector)
                """;

        return em.createQuery(query, DocumentEmbedding.class)
                .setParameter("organizationId", organizationId)
                .setParameter("queryVector", vectorString)
                .setMaxResults(limit)
                .getResultList();
    }

    private String arrayToVectorString(float[] array) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0)
                sb.append(",");
            sb.append(array[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    public long deleteByDocument(UUID documentId) {
        return delete("document.id", documentId);
    }
}
