package com.platform.service;

import com.platform.domain.Agent;
import com.platform.domain.Conversation;
import com.platform.domain.Message;
import com.platform.domain.User;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic tests for AgentRuntimeService.
 */
@QuarkusTest
public class AgentRuntimeServiceTest {

    @Inject
    AgentRuntimeService runtimeService;

    @Test
    public void testLoadAgentConfiguration() {
        // This test verifies the agent loading method exists and can be called
        // In a real scenario, we would set up test data
        UUID testAgentId = UUID.randomUUID();
        Agent agent = runtimeService.loadAgentConfiguration(testAgentId);

        // Agent won't exist, so it should be null
        assertNull(agent, "Non-existent agent should return null");
    }

    @Test
    public void testBuildConversationContext() {
        // Test that conversation context building works with empty conversation
        UUID testConversationId = UUID.randomUUID();
        String context = runtimeService.buildConversationContext(testConversationId);

        // Should return empty string for non-existent conversation
        assertNotNull(context, "Context should not be null");
        assertEquals("", context, "Context should be empty for non-existent conversation");
    }

    @Test
    @Transactional
    public void testGetOrCreateConversation_CreatesNew() {
        // Create a test agent and user
        Agent agent = new Agent();
        agent.name = "Test Agent";
        agent.systemPrompt = "You are a helpful assistant";
        agent.status = Agent.AgentStatus.ACTIVE;
        agent.modelName = "mistral-small";
        agent.persist();

        User user = new User();
        user.email = "test@example.com";
        user.passwordHash = "hash";
        user.persist();

        // Test creating a new conversation
        Conversation conversation = runtimeService.getOrCreateConversation(null, agent, user.id);

        assertNotNull(conversation, "Conversation should be created");
        assertNotNull(conversation.id, "Conversation should have an ID");
        assertEquals(agent.id, conversation.agent.id, "Conversation should be linked to agent");
        assertEquals(Conversation.ConversationStatus.ACTIVE, conversation.status, "Conversation should be active");
    }
}
