package com.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.domain.Agent;
import com.platform.domain.AgentTool;
import com.platform.domain.Tool;
import com.platform.domain.User;
import com.platform.exception.ValidationException;
import com.platform.repository.AgentRepository;
import com.platform.repository.UserRepository;
import com.platform.service.dto.*;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class AgentWizardService {

    private static final long SESSION_EXPIRATION_SECONDS = 7200L; // 2 hours

    @Inject
    RedisDataSource redisDataSource;

    @Inject
    UserRepository userRepository;

    @Inject
    AgentRepository agentRepository;

    @Inject
    ObjectMapper objectMapper;

    private ValueCommands<String, String> sessionCommands;

    @jakarta.annotation.PostConstruct
    void init() {
        sessionCommands = redisDataSource.value(String.class);
    }

    public AgentWizardSession createSession(UUID userId) {
        if (userId == null) {
            throw new ValidationException("User ID is required");
        }

        User user = userRepository.findById(userId);
        if (user == null) {
            throw new ValidationException("User not found");
        }

        AgentWizardSession session = new AgentWizardSession(userId);
        saveSessionToRedis(session);

        return session;
    }

    public void saveStep(UUID sessionId, AgentWizardSession.WizardStep step, Map<String, Object> data) {
        if (sessionId == null) {
            throw new ValidationException("Session ID is required");
        }

        AgentWizardSession session = getSession(sessionId);
        if (session == null) {
            throw new ValidationException("Session not found or expired");
        }

        session.stepData.putAll(data);
        session.currentStep = step;

        saveSessionToRedis(session);
    }

    public AgentWizardSession getSession(UUID sessionId) {
        if (sessionId == null) {
            return null;
        }

        String sessionKey = "wizard:session:" + sessionId.toString();
        String sessionJson = sessionCommands.get(sessionKey);

        if (sessionJson == null) {
            return null;
        }

        try {
            return objectMapper.readValue(sessionJson, AgentWizardSession.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize wizard session", e);
        }
    }

    public ValidationResult validateConfiguration(AgentConfiguration config) {
        ValidationResult result = new ValidationResult();

        if (config == null) {
            result.addError("Configuration is required");
            return result;
        }

        // Validate agent name
        if (config.name == null || config.name.isBlank()) {
            result.addError("Agent name is required");
        } else if (config.name.length() < 3) {
            result.addError("Agent name must be at least 3 characters");
        } else if (config.name.length() > 255) {
            result.addError("Agent name must not exceed 255 characters");
        }

        // Validate description
        if (config.description != null && config.description.length() > 1000) {
            result.addError("Description must not exceed 1000 characters");
        }

        // Validate system prompt
        if (config.systemPrompt == null || config.systemPrompt.isBlank()) {
            result.addError("System prompt is required");
        } else if (config.systemPrompt.length() < 10) {
            result.addError("System prompt must be at least 10 characters");
        } else if (config.systemPrompt.length() > 10000) {
            result.addError("System prompt must not exceed 10000 characters");
        }

        // Validate model name
        if (config.modelName == null || config.modelName.isBlank()) {
            result.addError("Model name is required");
        }

        return result;
    }

    @Transactional
    public Agent deployAgent(AgentConfiguration config, UUID userId) {
        if (userId == null) {
            throw new ValidationException("User ID is required");
        }

        ValidationResult validation = validateConfiguration(config);
        if (!validation.valid) {
            throw new ValidationException("Invalid configuration: " + String.join(", ", validation.errors));
        }

        User user = userRepository.findByIdWithOrganization(userId)
                .orElseThrow(() -> new ValidationException("User not found"));

        Agent agent = new Agent();
        agent.name = config.name.trim();
        agent.description = config.description != null ? config.description.trim() : null;
        agent.systemPrompt = config.systemPrompt.trim();
        agent.modelName = config.modelName;
        agent.owner = user;
        agent.organization = user.organization;
        agent.status = Agent.AgentStatus.ACTIVE;

        // Add tools if specified
        if (config.toolIds != null && !config.toolIds.isEmpty()) {
            for (UUID toolId : config.toolIds) {
                Tool tool = Tool.findById(toolId);
                if (tool != null) {
                    AgentTool agentTool = new AgentTool(agent, tool);
                    agent.tools.add(agentTool);
                }
            }
        }

        agentRepository.persist(agent);

        return agent;
    }

    public PreviewResponse previewAgent(AgentConfiguration config, String testPrompt) {
        long startTime = System.currentTimeMillis();

        try {
            ValidationResult validation = validateConfiguration(config);
            if (!validation.valid) {
                return new PreviewResponse("Configuration validation failed: " + String.join(", ", validation.errors));
            }

            if (testPrompt == null || testPrompt.isBlank()) {
                return new PreviewResponse("Test prompt is required");
            }

            // For now, return a mock response
            // In a real implementation, this would call the AI service
            String mockResponse = String.format(
                    "Preview response from agent '%s': This is a simulated response to your test prompt. " +
                            "The agent would process your message using the configured system prompt and respond accordingly.",
                    config.name);

            long responseTime = System.currentTimeMillis() - startTime;
            return new PreviewResponse(mockResponse, responseTime);

        } catch (Exception e) {
            return new PreviewResponse("Preview failed: " + e.getMessage());
        }
    }

    public void deleteSession(UUID sessionId) {
        if (sessionId != null) {
            String sessionKey = "wizard:session:" + sessionId.toString();
            sessionCommands.getdel(sessionKey);
        }
    }

    private void saveSessionToRedis(AgentWizardSession session) {
        try {
            String sessionKey = "wizard:session:" + session.sessionId.toString();
            String sessionJson = objectMapper.writeValueAsString(session);
            sessionCommands.setex(sessionKey, SESSION_EXPIRATION_SECONDS, sessionJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize wizard session", e);
        }
    }
}
