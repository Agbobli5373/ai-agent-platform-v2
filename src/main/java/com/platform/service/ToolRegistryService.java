package com.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.domain.Tool;
import com.platform.domain.User;
import com.platform.exception.ValidationException;
import com.platform.repository.ToolRepository;
import com.platform.service.dto.ToolExecutionResult;
import com.platform.service.dto.ToolRegistrationRequest;
import com.platform.service.dto.ToolValidationResult;
import io.smallrye.faulttolerance.api.CircuitBreakerName;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Service for managing tool registry and discovery.
 * Handles tool registration, validation, and discovery for agents.
 */
@ApplicationScoped
public class ToolRegistryService {

    private static final Logger LOG = Logger.getLogger(ToolRegistryService.class);

    @Inject
    ToolRepository toolRepository;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    ToolExecutor toolExecutor;

    private final HttpClient httpClient;

    public ToolRegistryService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Register a new tool.
     *
     * @param request the tool registration request
     * @param userId  the user ID registering the tool
     * @param orgId   the organization ID
     * @return the registered tool
     */
    @Transactional
    public Tool registerTool(ToolRegistrationRequest request, UUID userId, UUID orgId) {
        LOG.infof("Registering tool: %s for user: %s", request.name, userId);

        // Validate request
        validateToolRequest(request);

        // Fetch user
        User owner = User.findById(userId);
        if (owner == null) {
            throw new ValidationException("User not found");
        }

        // Create tool entity
        Tool tool = new Tool();
        tool.name = request.name;
        tool.description = request.description;
        tool.type = request.type;
        tool.endpoint = request.endpoint;
        tool.owner = owner;

        // Serialize auth config and parameters to JSON
        try {
            if (request.authConfig != null) {
                tool.authConfig = objectMapper.writeValueAsString(request.authConfig);
            }
            if (request.parameters != null) {
                tool.parameters = objectMapper.writeValueAsString(request.parameters);
            }
        } catch (JsonProcessingException e) {
            throw new ValidationException("Failed to serialize tool configuration: " + e.getMessage());
        }

        toolRepository.persist(tool);
        LOG.infof("Tool registered successfully with ID: %s", tool.id);

        return tool;
    }

    /**
     * Validate tool connectivity.
     *
     * @param toolId the tool ID
     * @return validation result
     */
    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    public ToolValidationResult validateConnection(UUID toolId) {
        LOG.infof("Validating connection for tool: %s", toolId);

        Optional<Tool> toolOpt = toolRepository.findByIdOptional(toolId);
        if (toolOpt.isEmpty()) {
            return ToolValidationResult.failure("Tool not found", "Tool with ID " + toolId + " does not exist");
        }

        Tool tool = toolOpt.get();
        long startTime = System.currentTimeMillis();

        try {
            // Build HTTP request
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(tool.endpoint))
                    .timeout(Duration.ofSeconds(5));

            // Add authentication headers
            addAuthenticationHeaders(requestBuilder, tool);

            // For validation, use GET or HEAD request
            HttpRequest request = requestBuilder.GET().build();

            // Execute request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long responseTime = System.currentTimeMillis() - startTime;

            // Check response status
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return ToolValidationResult.success(
                        "Connection successful",
                        responseTime,
                        response.statusCode());
            } else if (response.statusCode() == 401 || response.statusCode() == 403) {
                return ToolValidationResult.failure(
                        "Authentication failed",
                        "Status code: " + response.statusCode());
            } else {
                return ToolValidationResult.failure(
                        "Connection failed",
                        "Status code: " + response.statusCode());
            }

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            LOG.errorf(e, "Failed to validate tool connection: %s", toolId);
            return ToolValidationResult.failure(
                    "Connection error: " + e.getMessage(),
                    e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * Discover tools available for an agent.
     *
     * @param agentId        the agent ID (currently returns all tools for the
     *                       organization)
     * @param organizationId the organization ID
     * @return list of available tools
     */
    public List<Tool> discoverTools(UUID agentId, UUID organizationId) {
        LOG.infof("Discovering tools for agent: %s in organization: %s", agentId, organizationId);
        return toolRepository.findByOrganization(organizationId);
    }

    /**
     * Get all tools for a user.
     *
     * @param owner the user
     * @return list of tools
     */
    public List<Tool> getToolsByOwner(User owner) {
        return toolRepository.findByOwner(owner);
    }

    /**
     * Get all tools for an organization.
     *
     * @param organizationId the organization ID
     * @return list of tools
     */
    public List<Tool> getToolsByOrganization(UUID organizationId) {
        return toolRepository.findByOrganization(organizationId);
    }

    /**
     * Get a tool by ID.
     *
     * @param toolId the tool ID
     * @return optional containing the tool
     */
    public Optional<Tool> getToolById(UUID toolId) {
        return toolRepository.findByIdOptional(toolId);
    }

    /**
     * Update a tool.
     *
     * @param toolId  the tool ID
     * @param request the update request
     * @param userId  the user ID
     * @param orgId   the organization ID
     * @return the updated tool
     */
    @Transactional
    public Tool updateTool(UUID toolId, ToolRegistrationRequest request, UUID userId, UUID orgId) {
        LOG.infof("Updating tool: %s", toolId);

        Optional<Tool> toolOpt = toolRepository.findByIdOptional(toolId);
        if (toolOpt.isEmpty()) {
            throw new ValidationException("Tool not found");
        }

        Tool tool = toolOpt.get();
        
        // Verify ownership
        if (tool.owner == null || !tool.owner.id.equals(userId)) {
            throw new ValidationException("Access denied");
        }

        validateToolRequest(request);

        tool.name = request.name;
        tool.description = request.description;
        tool.type = request.type;
        tool.endpoint = request.endpoint;

        try {
            if (request.authConfig != null) {
                tool.authConfig = objectMapper.writeValueAsString(request.authConfig);
            }
            if (request.parameters != null) {
                tool.parameters = objectMapper.writeValueAsString(request.parameters);
            }
        } catch (JsonProcessingException e) {
            throw new ValidationException("Failed to serialize tool configuration: " + e.getMessage());
        }

        toolRepository.persist(tool);
        LOG.infof("Tool updated successfully: %s", toolId);

        return tool;
    }

    /**
     * Delete a tool.
     *
     * @param toolId the tool ID
     * @param userId the user ID
     * @param orgId  the organization ID
     */
    @Transactional
    public void deleteTool(UUID toolId, UUID userId, UUID orgId) {
        LOG.infof("Deleting tool: %s", toolId);

        Optional<Tool> toolOpt = toolRepository.findByIdOptional(toolId);
        if (toolOpt.isEmpty()) {
            throw new ValidationException("Tool not found");
        }

        Tool tool = toolOpt.get();
        
        // Verify ownership (admins can delete any tool in their org)
        if (tool.owner != null && tool.owner.organization != null && 
            !tool.owner.organization.id.equals(orgId)) {
            throw new ValidationException("Access denied");
        }

        toolRepository.delete(tool);
        LOG.infof("Tool deleted successfully: %s", toolId);
    }

    /**
     * Execute a tool with given parameters.
     *
     * @param toolId the tool ID
     * @param params the execution parameters
     * @return execution result
     */
    public ToolExecutionResult executeTool(UUID toolId, Map<String, Object> params) {
        LOG.infof("Executing tool: %s with params: %s", toolId, params);

        Optional<Tool> toolOpt = toolRepository.findByIdOptional(toolId);
        if (toolOpt.isEmpty()) {
            return ToolExecutionResult.failure("Tool not found", 0);
        }

        Tool tool = toolOpt.get();
        return toolExecutor.execute(tool, params);
    }

    // Private helper methods

    private void validateToolRequest(ToolRegistrationRequest request) {
        if (request.name == null || request.name.trim().isEmpty()) {
            throw new ValidationException("Tool name is required");
        }
        if (request.type == null) {
            throw new ValidationException("Tool type is required");
        }
        if (request.endpoint == null || request.endpoint.trim().isEmpty()) {
            throw new ValidationException("Tool endpoint is required");
        }

        // Validate endpoint URL format
        try {
            URI.create(request.endpoint);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid endpoint URL format");
        }
    }

    private void addAuthenticationHeaders(HttpRequest.Builder requestBuilder, Tool tool) {
        if (tool.authConfig == null || tool.authConfig.isEmpty()) {
            return;
        }

        try {
            ToolRegistrationRequest.AuthenticationConfig authConfig = objectMapper.readValue(tool.authConfig,
                    ToolRegistrationRequest.AuthenticationConfig.class);

            switch (authConfig.type) {
                case API_KEY:
                    if (authConfig.apiKey != null) {
                        requestBuilder.header("Authorization", "Bearer " + authConfig.apiKey);
                    }
                    break;
                case BASIC_AUTH:
                    if (authConfig.username != null && authConfig.password != null) {
                        String credentials = authConfig.username + ":" + authConfig.password;
                        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
                        requestBuilder.header("Authorization", "Basic " + encodedCredentials);
                    }
                    break;
                case OAUTH2:
                    // OAuth2 token handling would require a more complex implementation
                    LOG.warn("OAuth2 authentication not fully implemented yet");
                    break;
                case NONE:
                default:
                    // No authentication
                    break;
            }
        } catch (JsonProcessingException e) {
            LOG.errorf(e, "Failed to parse auth config for tool: %s", tool.id);
        }
    }

    private String buildUrlWithParams(String baseUrl, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return baseUrl;
        }

        // For GET requests, append query parameters
        StringBuilder url = new StringBuilder(baseUrl);
        if (!baseUrl.contains("?")) {
            url.append("?");
        } else {
            url.append("&");
        }

        params.forEach((key, value) -> {
            url.append(key).append("=").append(value).append("&");
        });

        // Remove trailing &
        return url.substring(0, url.length() - 1);
    }

    private Object parseResponse(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return null;
        }

        try {
            // Try to parse as JSON
            return objectMapper.readValue(responseBody, Object.class);
        } catch (JsonProcessingException e) {
            // Return as plain text if not JSON
            return responseBody;
        }
    }
}
