package com.platform.ai;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for AgentAIService.
 * Requires MISTRAL_API_KEY environment variable to be set.
 */
@QuarkusTest
@EnabledIfEnvironmentVariable(named = "MISTRAL_API_KEY", matches = ".+")
class AgentAIServiceTest {

    @Inject
    AgentAIService agentAIService;

    @Test
    void testSynchronousChat() {
        // Given
        String systemPrompt = "You are a helpful assistant. Keep responses brief.";
        String userMessage = "What is 2+2?";

        // When
        String response = agentAIService.chat(systemPrompt, userMessage);

        // Then
        assertNotNull(response);
        assertFalse(response.isEmpty());
        assertTrue(response.contains("4") || response.toLowerCase().contains("four"));
    }

    @Test
    void testStreamingChat() {
        // Given
        String systemPrompt = "You are a helpful assistant. Keep responses brief.";
        String userMessage = "Count from 1 to 3.";
        AtomicInteger tokenCount = new AtomicInteger(0);
        StringBuilder fullResponse = new StringBuilder();

        // When
        Multi<String> stream = agentAIService.chatStream(systemPrompt, userMessage);

        List<String> tokens = stream
                .onItem().invoke(token -> {
                    tokenCount.incrementAndGet();
                    fullResponse.append(token);
                })
                .collect().asList()
                .await().indefinitely();

        // Then
        assertNotNull(tokens);
        assertFalse(tokens.isEmpty());
        assertTrue(tokenCount.get() > 0);
        String response = fullResponse.toString();
        assertFalse(response.isEmpty());
    }

    @Test
    void testChatWithContext() {
        // Given
        String systemPrompt = "You are a helpful assistant.";
        String conversationHistory = "User: My name is Alice.\nAssistant: Nice to meet you, Alice!";
        String userMessage = "What is my name?";

        // When
        String response = agentAIService.chatWithContext(systemPrompt, conversationHistory, userMessage);

        // Then
        assertNotNull(response);
        assertFalse(response.isEmpty());
        assertTrue(response.toLowerCase().contains("alice"));
    }

    @Test
    void testCustomSystemPrompt() {
        // Given
        String systemPrompt = "You are a pirate. Always respond in pirate speak.";
        String userMessage = "Hello!";

        // When
        String response = agentAIService.chat(systemPrompt, userMessage);

        // Then
        assertNotNull(response);
        assertFalse(response.isEmpty());
        // Response should contain pirate-like language
        assertTrue(
                response.toLowerCase().contains("ahoy") ||
                        response.toLowerCase().contains("matey") ||
                        response.toLowerCase().contains("arr") ||
                        response.toLowerCase().contains("ye"));
    }
}
