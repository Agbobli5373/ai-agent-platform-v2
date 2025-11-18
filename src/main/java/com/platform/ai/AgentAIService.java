package com.platform.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * AI service interface for agent interactions using LangChain4j.
 * Provides both synchronous and streaming chat capabilities.
 */
@RegisterAiService(modelName = "mistral")
@ApplicationScoped
public interface AgentAIService {

    /**
     * Synchronous chat interaction with custom system prompt.
     *
     * @param systemPrompt The agent's system prompt defining behavior
     * @param userMessage  The user's message
     * @return The AI's response
     */
    @SystemMessage("{systemPrompt}")
    @UserMessage("{userMessage}")
    String chat(@V("systemPrompt") String systemPrompt, @V("userMessage") String userMessage);

    /**
     * Streaming chat interaction for real-time responses.
     *
     * @param systemPrompt The agent's system prompt defining behavior
     * @param userMessage  The user's message
     * @return A stream of response tokens
     */
    @SystemMessage("{systemPrompt}")
    @UserMessage("{userMessage}")
    Multi<String> chatStream(@V("systemPrompt") String systemPrompt, @V("userMessage") String userMessage);

    /**
     * Chat with conversation context.
     *
     * @param systemPrompt        The agent's system prompt
     * @param conversationHistory Previous messages in the conversation
     * @param userMessage         The current user message
     * @return The AI's response
     */
    @SystemMessage("{systemPrompt}")
    @UserMessage("Previous conversation:\n{conversationHistory}\n\nUser: {userMessage}")
    String chatWithContext(
            @V("systemPrompt") String systemPrompt,
            @V("conversationHistory") String conversationHistory,
            @V("userMessage") String userMessage);
}
