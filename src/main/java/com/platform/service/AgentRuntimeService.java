package com.platform.service;

import com.platform.ai.AgentAIService;
import com.platform.domain.*;
import com.platform.repository.MessageRepository;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Agent Runtime Service handles agent execution, message processing,
 * and conversation management.
 */
@ApplicationScoped
public class AgentRuntimeService {

    @Inject
    AgentAIService aiService;

    @Inject
    MessageRepository messageRepository;

    @Inject
    ToolExecutionOrchestrator toolOrchestrator;

    /**
     * Process a message synchronously with conversation context.
     *
     * @param agentId        The agent ID
     * @param userMessage    The user's message
     * @param conversationId The conversation ID (optional, creates new if null)
     * @param userId         The user ID
     * @return AgentResponse with the AI's reply
     */
    @Transactional
    public CompletionStage<AgentResponse> processMessage(
            UUID agentId,
            String userMessage,
            UUID conversationId,
            UUID userId) {

        return Uni.createFrom().item(() -> {
            // Load agent configuration
            Agent agent = loadAgentConfiguration(agentId);
            if (agent == null) {
                throw new IllegalArgumentException("Agent not found: " + agentId);
            }

            if (agent.status != Agent.AgentStatus.ACTIVE) {
                throw new IllegalStateException("Agent is not active: " + agent.status);
            }

            // Get or create conversation
            Conversation conversation = getOrCreateConversation(conversationId, agent, userId);

            // Save user message
            Message userMsg = new Message();
            userMsg.conversation = conversation;
            userMsg.role = Message.MessageRole.USER;
            userMsg.content = userMessage;
            userMsg.timestamp = LocalDateTime.now();
            userMsg.persist();

            // Build conversation context
            String conversationHistory = buildConversationContext(conversation.id);

            // Get AI response
            String aiResponse;
            if (conversationHistory.isEmpty()) {
                aiResponse = aiService.chat(agent.systemPrompt, userMessage);
            } else {
                aiResponse = aiService.chatWithContext(agent.systemPrompt, conversationHistory, userMessage);
            }

            // Save assistant message
            Message assistantMsg = new Message();
            assistantMsg.conversation = conversation;
            assistantMsg.role = Message.MessageRole.ASSISTANT;
            assistantMsg.content = aiResponse;
            assistantMsg.timestamp = LocalDateTime.now();
            assistantMsg.persist();

            // Update conversation
            conversation.messages.add(userMsg);
            conversation.messages.add(assistantMsg);

            Log.infof("Processed message for agent %s, conversation %s", agentId, conversation.id);

            return new AgentResponse(
                    conversation.id,
                    aiResponse,
                    assistantMsg.id,
                    LocalDateTime.now());
        }).subscribeAsCompletionStage();
    }

    /**
     * Stream a response for real-time chat.
     *
     * @param agentId        The agent ID
     * @param userMessage    The user's message
     * @param conversationId The conversation ID (optional)
     * @param userId         The user ID
     * @return Multi stream of response tokens
     */
    public Multi<String> streamResponse(
            UUID agentId,
            String userMessage,
            UUID conversationId,
            UUID userId) {

        return Multi.createFrom().emitter(emitter -> {
            try {
                // Load agent configuration
                Agent agent = loadAgentConfiguration(agentId);
                if (agent == null) {
                    emitter.fail(new IllegalArgumentException("Agent not found: " + agentId));
                    return;
                }

                if (agent.status != Agent.AgentStatus.ACTIVE) {
                    emitter.fail(new IllegalStateException("Agent is not active: " + agent.status));
                    return;
                }

                // Get or create conversation (in transaction)
                Conversation conversation = getOrCreateConversation(conversationId, agent, userId);

                // Save user message (in transaction)
                saveUserMessage(conversation, userMessage);

                // Stream AI response
                Multi<String> stream = aiService.chatStream(agent.systemPrompt, userMessage);

                StringBuilder fullResponse = new StringBuilder();

                stream.subscribe().with(
                        token -> {
                            fullResponse.append(token);
                            emitter.emit(token);
                        },
                        failure -> {
                            Log.errorf(failure, "Error streaming response for agent %s", agentId);
                            emitter.fail(failure);
                        },
                        () -> {
                            // Save complete assistant message
                            saveAssistantMessage(conversation, fullResponse.toString());
                            emitter.complete();
                            Log.infof("Completed streaming for agent %s, conversation %s",
                                    agentId, conversation.id);
                        });

            } catch (Exception e) {
                Log.errorf(e, "Error in streamResponse for agent %s", agentId);
                emitter.fail(e);
            }
        });
    }

    /**
     * Load agent configuration from database.
     *
     * @param agentId The agent ID
     * @return Agent entity with configuration
     */
    public Agent loadAgentConfiguration(UUID agentId) {
        return Agent.findById(agentId);
    }

    /**
     * Get existing conversation or create a new one.
     *
     * @param conversationId The conversation ID (null to create new)
     * @param agent          The agent
     * @param userId         The user ID
     * @return Conversation entity
     */
    @Transactional
    public Conversation getOrCreateConversation(UUID conversationId, Agent agent, UUID userId) {
        if (conversationId != null) {
            Conversation existing = Conversation.findById(conversationId);
            if (existing != null) {
                return existing;
            }
        }

        // Create new conversation
        Conversation conversation = new Conversation();
        conversation.agent = agent;
        conversation.user = User.findById(userId);
        conversation.startedAt = LocalDateTime.now();
        conversation.createdAt = LocalDateTime.now();
        conversation.status = Conversation.ConversationStatus.ACTIVE;
        conversation.persist();

        Log.infof("Created new conversation %s for agent %s", conversation.id, agent.id);
        return conversation;
    }

    /**
     * Build conversation context from message history.
     *
     * @param conversationId The conversation ID
     * @return Formatted conversation history
     */
    public String buildConversationContext(UUID conversationId) {
        List<Message> messages = messageRepository.list(
                "conversation.id = ?1 ORDER BY timestamp ASC",
                conversationId);

        if (messages.isEmpty()) {
            return "";
        }

        // Limit to last 10 messages to avoid context overflow
        int startIndex = Math.max(0, messages.size() - 10);
        List<Message> recentMessages = messages.subList(startIndex, messages.size());

        return recentMessages.stream()
                .map(msg -> {
                    String roleLabel = msg.role == Message.MessageRole.USER ? "User" : "Assistant";
                    return roleLabel + ": " + msg.content;
                })
                .collect(Collectors.joining("\n"));
    }

    /**
     * Save user message in a transaction.
     */
    @Transactional
    void saveUserMessage(Conversation conversation, String content) {
        Message userMsg = new Message();
        userMsg.conversation = conversation;
        userMsg.role = Message.MessageRole.USER;
        userMsg.content = content;
        userMsg.timestamp = LocalDateTime.now();
        userMsg.persist();
    }

    /**
     * Save assistant message in a transaction.
     */
    @Transactional
    void saveAssistantMessage(Conversation conversation, String content) {
        Message assistantMsg = new Message();
        assistantMsg.conversation = conversation;
        assistantMsg.role = Message.MessageRole.ASSISTANT;
        assistantMsg.content = content;
        assistantMsg.timestamp = LocalDateTime.now();
        assistantMsg.persist();
    }

    /**
     * Execute tools for an agent with orchestration.
     *
     * @param agent   The agent
     * @param toolIds List of tool IDs to execute
     * @param context Execution context
     * @return Tool chain execution result
     */
    public CompletionStage<ToolExecutionOrchestrator.ToolChainResult> executeTools(
            Agent agent,
            List<String> toolIds,
            Map<String, Object> context) {

        return toolOrchestrator.executeToolChain(agent, toolIds, context);
    }

    /**
     * Response DTO for agent interactions.
     */
    public static class AgentResponse {
        public UUID conversationId;
        public String content;
        public UUID messageId;
        public LocalDateTime timestamp;

        public AgentResponse(UUID conversationId, String content, UUID messageId, LocalDateTime timestamp) {
            this.conversationId = conversationId;
            this.content = content;
            this.messageId = messageId;
            this.timestamp = timestamp;
        }
    }
}
