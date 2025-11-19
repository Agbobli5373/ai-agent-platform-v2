package com.platform.service;

import com.platform.domain.Document;
import com.platform.domain.DocumentEmbedding;
import com.platform.repository.DocumentRepository;
import com.platform.repository.DocumentEmbeddingRepository;
import com.platform.ai.EmbeddingService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.ArrayList;import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class VectorStoreService {

    private static final Logger LOG = Logger.getLogger(VectorStoreService.class);
    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;

    @Inject
    DocumentRepository documentRepository;

    @Inject
    DocumentEmbeddingRepository embeddingRepository;

    @Inject
    EmbeddingService embeddingService;

    @Transactional
    public void indexDocument(Document document, String content) {
        LOG.infof("Starting indexing for document: %s", document.filename);
        
        try {
            document.status = Document.DocumentStatus.PENDING;
            documentRepository.persist(document);

            List<String> chunks = chunkDocument(content);
            LOG.infof("Document chunked into %d pieces", chunks.size());

            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                float[] embedding = embeddingService.embed(chunk);

                DocumentEmbedding docEmbedding = new DocumentEmbedding();
                docEmbedding.document = document;
                docEmbedding.chunkIndex = i;
                docEmbedding.content = chunk;
                docEmbedding.embedding = embedding;
                
                embeddingRepository.persist(docEmbedding);
            }

            document.status = Document.DocumentStatus.INDEXED;
            document.indexedAt = LocalDateTime.now();
            documentRepository.persist(document);

            LOG.infof("Document indexed successfully: %s", document.filename);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to index document: %s", document.filename);
            document.status = Document.DocumentStatus.FAILED;
            documentRepository.persist(document);
            throw new RuntimeException("Document indexing failed", e);
        }
    }

    public List<String> chunkDocument(String content) {
        List<String> chunks = new ArrayList<>();
        
        if (content == null || content.isEmpty()) {
            return chunks;
        }

        String[] words = content.split("\\s+");
        
        for (int i = 0; i < words.length; i += (CHUNK_SIZE - CHUNK_OVERLAP)) {
            int end = Math.min(i + CHUNK_SIZE, words.length);
            String chunk = String.join(" ", java.util.Arrays.copyOfRange(words, i, end));
            chunks.add(chunk);
            
            if (end >= words.length) {
                break;
            }
        }

        return chunks;
    }

    public List<SearchResult> semanticSearch(String query, UUID organizationId, int limit) {
        return semanticSearch(query, organizationId, limit, 0.0);
    }

    /**
     * Perform semantic search with relevance threshold filtering.
     *
     * @param query            The search query
     * @param organizationId   The organization ID for filtering
     * @param limit            Maximum number of results
     * @param relevanceThreshold Minimum relevance score (0.0 to 1.0)
     * @return List of search results above the threshold
     */
    public List<SearchResult> semanticSearch(String query, UUID organizationId, int limit, double relevanceThreshold) {
        LOG.infof("Performing semantic search for query: %s with threshold: %.2f", query, relevanceThreshold);
        
        float[] queryEmbedding = embeddingService.embed(query);
        
        // Retrieve more results than needed to account for filtering
        int retrievalLimit = Math.min(limit * 3, 50);
        List<DocumentEmbedding> results = embeddingRepository.findSimilar(
            queryEmbedding, 
            organizationId, 
            retrievalLimit
        );

        List<SearchResult> searchResults = new ArrayList<>();
        for (DocumentEmbedding embedding : results) {
            double relevanceScore = calculateRelevance(queryEmbedding, embedding.embedding);
            
            // Apply relevance threshold filtering
            if (relevanceScore >= relevanceThreshold) {
                SearchResult result = new SearchResult();
                result.documentId = embedding.document.id;
                result.documentName = embedding.document.filename;
                result.chunkText = embedding.content;
                result.chunkIndex = embedding.chunkIndex;
                result.relevanceScore = relevanceScore;
                searchResults.add(result);
                
                // Stop once we have enough results
                if (searchResults.size() >= limit) {
                    break;
                }
            }
        }

        LOG.infof("Found %d results above threshold %.2f", searchResults.size(), relevanceThreshold);
        return searchResults;
    }

    private double calculateRelevance(float[] embedding1, float[] embedding2) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < embedding1.length; i++) {
            dotProduct += embedding1[i] * embedding2[i];
            norm1 += embedding1[i] * embedding1[i];
            norm2 += embedding2[i] * embedding2[i];
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    public static class SearchResult {
        public UUID documentId;
        public String documentName;
        public String chunkText;
        public int chunkIndex;
        public double relevanceScore;
    }
}
