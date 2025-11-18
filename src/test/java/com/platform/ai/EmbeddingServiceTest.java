package com.platform.ai;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for EmbeddingService.
 * Requires MISTRAL_API_KEY environment variable to be set.
 */
@QuarkusTest
@EnabledIfEnvironmentVariable(named = "MISTRAL_API_KEY", matches = ".+")
class EmbeddingServiceTest {

    @Inject
    EmbeddingService embeddingService;

    @Test
    void testEmbedSingleText() {
        // Given
        String text = "This is a test document about artificial intelligence.";

        // When
        float[] embedding = embeddingService.embed(text);

        // Then
        assertNotNull(embedding);
        assertEquals(1024, embedding.length); // Mistral embed produces 1024-dimensional vectors

        // Check that embedding contains non-zero values
        boolean hasNonZero = false;
        for (float value : embedding) {
            if (value != 0.0f) {
                hasNonZero = true;
                break;
            }
        }
        assertTrue(hasNonZero);
    }

    @Test
    void testEmbedMultipleTexts() {
        // Given
        List<String> texts = Arrays.asList(
                "Machine learning is a subset of AI.",
                "Natural language processing enables computers to understand text.",
                "Deep learning uses neural networks.");

        // When
        List<float[]> embeddings = embeddingService.embedAll(texts);

        // Then
        assertNotNull(embeddings);
        assertEquals(3, embeddings.size());

        for (float[] embedding : embeddings) {
            assertNotNull(embedding);
            assertEquals(1024, embedding.length);
        }
    }

    @Test
    void testSimilarTextsShouldHaveSimilarEmbeddings() {
        // Given
        String text1 = "The cat sat on the mat.";
        String text2 = "A cat was sitting on a mat.";
        String text3 = "Quantum physics is complex.";

        // When
        float[] embedding1 = embeddingService.embed(text1);
        float[] embedding2 = embeddingService.embed(text2);
        float[] embedding3 = embeddingService.embed(text3);

        // Then
        double similarity12 = cosineSimilarity(embedding1, embedding2);
        double similarity13 = cosineSimilarity(embedding1, embedding3);

        // Similar texts should have higher similarity than dissimilar texts
        assertTrue(similarity12 > similarity13);
        assertTrue(similarity12 > 0.7); // Similar texts should have high similarity
    }

    @Test
    void testGetDimension() {
        // When
        int dimension = embeddingService.getDimension();

        // Then
        assertEquals(1024, dimension);
    }

    @Test
    void testEmbedEmptyText() {
        // Given
        String text = "";

        // When & Then
        assertThrows(RuntimeException.class, () -> embeddingService.embed(text));
    }

    /**
     * Calculate cosine similarity between two vectors.
     */
    private double cosineSimilarity(float[] vector1, float[] vector2) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += vector1[i] * vector1[i];
            norm2 += vector2[i] * vector2[i];
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}
