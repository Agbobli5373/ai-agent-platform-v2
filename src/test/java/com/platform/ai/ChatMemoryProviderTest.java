package com.platform.ai;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for RedisChatMemory.
 */
@QuarkusTest
class ChatMemoryProviderTest {

    @Inject
    RedisChatMemory chatMemoryProvider;

    private static final Long TEST_CONVERSATION_ID = 999L;

    @AfterEach
    void cleanup() {
        chatMemoryProvider.clearMemory(TEST_CONVERSATION_ID);
    }

    @Test
    void testStoreAndRetrieveMessage() {
        // Given
        String role = "user";
        String content = "Hello, how are you?";

        // When
        chatMemoryProvider.storeMessage(TEST_CONVERSATION_ID, role, content);
        List<String> messages = chatMemoryProvider.getMessages(TEST_CONVERSATION_ID);

        // Then
        assertNotNull(messages);
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).contains(role));
        assertTrue(messages.get(0).contains(content));
    }

    @Test
    void testStoreMultipleMessages() {
        // Given
        chatMemoryProvider.storeMessage(TEST_CONVERSATION_ID, "user", "Hello!");
        chatMemoryProvider.storeMessage(TEST_CONVERSATION_ID, "assistant", "Hi there!");
        chatMemoryProvider.storeMessage(TEST_CONVERSATION_ID, "user", "How are you?");

        // When
        List<String> messages = chatMemoryProvider.getMessages(TEST_CONVERSATION_ID);

        // Then
        assertNotNull(messages);
        assertEquals(3, messages.size());
        assertTrue(messages.get(0).contains("user: Hello!"));
        assertTrue(messages.get(1).contains("assistant: Hi there!"));
        assertTrue(messages.get(2).contains("user: How are you?"));
    }

    @Test
    void testGetConversationHistory() {
        // Given
        chatMemoryProvider.storeMessage(TEST_CONVERSATION_ID, "user", "What is AI?");
        chatMemoryProvider.storeMessage(TEST_CONVERSATION_ID, "assistant", "AI stands for Artificial Intelligence.");

        // When
        String history = chatMemoryProvider.getConversationHistory(TEST_CONVERSATION_ID);

        // Then
        assertNotNull(history);
        assertFalse(history.isEmpty());
        assertTrue(history.contains("user: What is AI?"));
        assertTrue(history.contains("assistant: AI stands for Artificial Intelligence."));
    }

    @Test
    void testClearMemory() {
        // Given
        chatMemoryProvider.storeMessage(TEST_CONVERSATION_ID, "user", "Test message");
        assertTrue(chatMemoryProvider.hasMemory(TEST_CONVERSATION_ID));

        // When
        chatMemoryProvider.clearMemory(TEST_CONVERSATION_ID);

        // Then
        assertFalse(chatMemoryProvider.hasMemory(TEST_CONVERSATION_ID));
        List<String> messages = chatMemoryProvider.getMessages(TEST_CONVERSATION_ID);
        assertTrue(messages.isEmpty());
    }

    @Test
    void testHasMemory() {
        // Given - no memory initially
        assertFalse(chatMemoryProvider.hasMemory(TEST_CONVERSATION_ID));

        // When
        chatMemoryProvider.storeMessage(TEST_CONVERSATION_ID, "user", "Test");

        // Then
        assertTrue(chatMemoryProvider.hasMemory(TEST_CONVERSATION_ID));
    }

    @Test
    void testUpdateTTL() {
        // Given
        chatMemoryProvider.storeMessage(TEST_CONVERSATION_ID, "user", "Test message");

        // When
        chatMemoryProvider.updateTTL(TEST_CONVERSATION_ID, Duration.ofMinutes(5));

        // Then
        assertTrue(chatMemoryProvider.hasMemory(TEST_CONVERSATION_ID));
        // Memory should still exist after TTL update
        List<String> messages = chatMemoryProvider.getMessages(TEST_CONVERSATION_ID);
        assertEquals(1, messages.size());
    }

    @Test
    void testEmptyConversation() {
        // When
        List<String> messages = chatMemoryProvider.getMessages(TEST_CONVERSATION_ID);
        String history = chatMemoryProvider.getConversationHistory(TEST_CONVERSATION_ID);

        // Then
        assertNotNull(messages);
        assertTrue(messages.isEmpty());
        assertNotNull(history);
        assertTrue(history.isEmpty());
    }
}
