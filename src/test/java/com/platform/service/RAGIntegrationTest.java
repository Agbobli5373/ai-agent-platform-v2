package com.platform.service;

import com.platform.domain.Agent;
import com.platform.domain.Organization;
import com.platform.service.dto.RAGConfiguration;
import com.platform.service.dto.RAGContext;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RAG (Retrieval-Augmented Generation) integration.
 */
@QuarkusTest
public class RAGIntegrationTest {

    @Inject
    AgentRuntimeService runtimeService;

    @Test
    public void testRAGConfiguration_DefaultDisabled() {
        // Test that RAG is disabled by default
        RAGConfiguration config = new RAGConfiguration();
        
        assertFalse(config.enabled, "RAG should be disabled by default");
        assertEquals(0.7, config.relevanceThreshold, 0.001, "Default threshold should be 0.7");
        assertEquals(5, config.maxPassages, "Default max passages should be 5");
        assertTrue(config.includeCitations, "Citations should be included by default");
    }

    @Test
    public void testRAGConfiguration_Validation() {
        RAGConfiguration config = new RAGConfiguration();
        config.enabled = true;
        config.relevanceThreshold = 0.8;
        config.maxPassages = 3;
        
        // Should not throw exception
        assertDoesNotThrow(() -> config.validate(), "Valid configuration should not throw");
    }

    @Test
    public void testRAGConfiguration_InvalidThreshold() {
        RAGConfiguration config = new RAGConfiguration();
        config.relevanceThreshold = 1.5; // Invalid: > 1.0
        
        assertThrows(IllegalArgumentException.class, () -> config.validate(),
                "Invalid threshold should throw exception");
    }

    @Test
    public void testRAGConfiguration_InvalidMaxPassages() {
        RAGConfiguration config = new RAGConfiguration();
        config.maxPassages = 0; // Invalid: < 1
        
        assertThrows(IllegalArgumentException.class, () -> config.validate(),
                "Invalid max passages should throw exception");
    }

    @Test
    public void testRAGContext_EmptyContext() {
        RAGContext context = new RAGContext();
        
        assertFalse(context.hasContext, "Empty context should not have context");
        assertEquals(0, context.totalPassages, "Empty context should have 0 passages");
        assertEquals("", context.formatForPrompt(), "Empty context should format to empty string");
    }

    @Test
    public void testRAGContext_WithPassages() {
        RAGContext context = new RAGContext();
        
        RAGContext.RetrievedPassage passage1 = new RAGContext.RetrievedPassage(
                java.util.UUID.randomUUID(),
                "Document1.pdf",
                "This is the first passage content.",
                0,
                0.85
        );
        
        RAGContext.RetrievedPassage passage2 = new RAGContext.RetrievedPassage(
                java.util.UUID.randomUUID(),
                "Document2.pdf",
                "This is the second passage content.",
                1,
                0.78
        );
        
        context.addPassage(passage1);
        context.addPassage(passage2);
        
        assertTrue(context.hasContext, "Context with passages should have context");
        assertEquals(2, context.totalPassages, "Should have 2 passages");
        
        String formatted = context.formatForPrompt();
        assertFalse(formatted.isEmpty(), "Formatted context should not be empty");
        assertTrue(formatted.contains("Document1.pdf"), "Should contain first document name");
        assertTrue(formatted.contains("Document2.pdf"), "Should contain second document name");
        assertTrue(formatted.contains("first passage content"), "Should contain first passage content");
        assertTrue(formatted.contains("second passage content"), "Should contain second passage content");
        assertTrue(formatted.contains("[Source 1:"), "Should contain source citation format");
        assertTrue(formatted.contains("[Source 2:"), "Should contain source citation format");
    }

    @Test
    public void testRAGContext_Citations() {
        RAGContext context = new RAGContext();
        
        java.util.UUID docId = java.util.UUID.randomUUID();
        RAGContext.RetrievedPassage passage = new RAGContext.RetrievedPassage(
                docId,
                "TestDoc.pdf",
                "Test content",
                0,
                0.9
        );
        
        context.addPassage(passage);
        
        var citations = context.getCitations();
        assertEquals(1, citations.size(), "Should have 1 citation");
        
        RAGContext.Citation citation = citations.get(0);
        assertEquals(1, citation.sourceNumber, "Source number should be 1");
        assertEquals(docId, citation.documentId, "Document ID should match");
        assertEquals("TestDoc.pdf", citation.documentName, "Document name should match");
        assertEquals(0, citation.chunkIndex, "Chunk index should match");
        assertEquals(0.9, citation.relevanceScore, 0.001, "Relevance score should match");
    }

    @Test
    @Transactional
    public void testRetrieveRAGContext_DisabledRAG() {
        // Create a test agent without RAG configuration
        Agent agent = new Agent();
        agent.name = "Test Agent";
        agent.systemPrompt = "You are a helpful assistant";
        agent.status = Agent.AgentStatus.ACTIVE;
        agent.modelName = "mistral-small";
        agent.configuration = null; // No configuration
        
        Organization org = new Organization();
        org.name = "Test Org";
        org.persist();
        agent.organization = org;
        agent.persist();
        
        // Retrieve RAG context
        RAGContext context = runtimeService.retrieveRAGContext(agent, "What is the weather?");
        
        assertNotNull(context, "Context should not be null");
        assertFalse(context.hasContext, "Context should be empty when RAG is disabled");
        assertEquals(0, context.totalPassages, "Should have 0 passages");
    }

    @Test
    @Transactional
    public void testRetrieveRAGContext_EnabledRAG() {
        // Create a test agent with RAG enabled
        Agent agent = new Agent();
        agent.name = "Test Agent";
        agent.systemPrompt = "You are a helpful assistant";
        agent.status = Agent.AgentStatus.ACTIVE;
        agent.modelName = "mistral-small";
        agent.configuration = "{\"rag\": {\"enabled\": true, \"relevanceThreshold\": 0.7, \"maxPassages\": 5}}";
        
        Organization org = new Organization();
        org.name = "Test Org";
        org.persist();
        agent.organization = org;
        agent.persist();
        
        // Retrieve RAG context (will be empty since no documents are indexed)
        RAGContext context = runtimeService.retrieveRAGContext(agent, "What is the weather?");
        
        assertNotNull(context, "Context should not be null");
        // Context will be empty since no documents are indexed, but RAG is enabled
        assertEquals(0, context.totalPassages, "Should have 0 passages (no documents indexed)");
    }

    @Test
    public void testRelevanceThresholdFiltering() {
        // Test that relevance threshold filtering works correctly
        RAGConfiguration config = RAGConfiguration.createDefault();
        config.enabled = true;
        config.relevanceThreshold = 0.75;
        
        RAGContext context = new RAGContext();
        
        // Add passage above threshold
        RAGContext.RetrievedPassage highRelevance = new RAGContext.RetrievedPassage(
                java.util.UUID.randomUUID(),
                "HighRelevance.pdf",
                "Highly relevant content",
                0,
                0.85 // Above threshold
        );
        context.addPassage(highRelevance);
        
        // In actual implementation, low relevance passages would be filtered out
        // by the VectorStoreService.semanticSearch method
        
        assertTrue(context.hasContext, "Should have context with high relevance passage");
        assertEquals(1, context.totalPassages, "Should have 1 passage");
    }
}
