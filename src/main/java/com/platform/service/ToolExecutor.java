package com.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.domain.Tool;
import com.platform.service.dto.ToolExecutionResult;
import com.platform.service.dto.ToolRegistrationRequest;
import io.smallrye.faulttolerance.api.CircuitBreakerName;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;

/**
 * Service for executing tools with REST client.
 * Handles HTTP requests with authentication, timeout, retry, and circuit
 * breaker patterns.
 */
@ApplicationScoped
public class ToolExecutor {

    private static final Logger LOG = Logger.getLogger(ToolExecutor.class);

    @Inject
    ObjectMapper objectMapper;

    private final HttpClient httpClient;

    public ToolExecutor() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Execute a tool with given parameters.
     * Includes timeout, retry, and circuit breaker fault tolerance.
     *
     * @param tool   the tool to execute
     * @param params the execution parameters
     * @return execution result
     */
    @Timeout(value = 10, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 3, delay = 1000, jitter = 500)
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 30000)
    @CircuitBreakerName("tool-execution")
    public ToolExecutionResult execute(Tool tool, Map<String, Object> params) {
        LOG.infof("Executing tool: %s (%s) with params: %s", tool.name, tool.id, params);

        long startTime = System.currentTimeMillis();

        try {
            // Build HTTP request
            HttpRequest request = buildHttpRequest(tool, params);

            // Execute request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long executionTime = System.currentTimeMillis() - startTime;

            // Process response
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                Object result = parseResponse(response.body());
                LOG.infof("Tool executed successfully: %s in %dms", tool.name, executionTime);

                ToolExecutionResult executionResult = ToolExecutionResult.success(result, executionTime);
                executionResult.metadata = Map.of(
                        "statusCode", response.statusCode(),
                        "toolId", tool.id.toString(),
                        "toolName", tool.name);
                return executionResult;
            } else {
                LOG.warnf("Tool execution failed with status: %d for tool: %s", response.statusCode(), tool.name);
                return ToolExecutionResult.failure(
                        "Tool execution failed with status: " + response.statusCode() + " - " + response.body(),
                        executionTime);
            }

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            LOG.errorf(e, "Failed to execute tool: %s", tool.name);
            return ToolExecutionResult.failure(
                    "Tool execution error: " + e.getMessage(),
                    executionTime);
        }
    }

    /**
     * Execute a tool with GET method (for simple queries).
     *
     * @param tool        the tool to execute
     * @param queryParams query parameters
     * @return execution result
     */
    @Timeout(value = 10, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 3, delay = 1000, jitter = 500)
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 30000)
    @CircuitBreakerName("tool-execution-get")
    public ToolExecutionResult executeGet(Tool tool, Map<String, String> queryParams) {
        LOG.infof("Executing GET request for tool: %s", tool.name);

        long startTime = System.currentTimeMillis();

        try {
            String url = buildUrlWithQueryParams(tool.endpoint, queryParams);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET();

            addAuthenticationHeaders(requestBuilder, tool);

            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long executionTime = System.currentTimeMillis() - startTime;

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                Object result = parseResponse(response.body());
                return ToolExecutionResult.success(result, executionTime);
            } else {
                return ToolExecutionResult.failure(
                        "GET request failed with status: " + response.statusCode(),
                        executionTime);
            }

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            LOG.errorf(e, "Failed to execute GET request for tool: %s", tool.name);
            return ToolExecutionResult.failure(
                    "GET request error: " + e.getMessage(),
                    executionTime);
        }
    }

    /**
     * Execute a tool with POST method.
     *
     * @param tool the tool to execute
     * @param body request body
     * @return execution result
     */
    @Timeout(value = 10, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 3, delay = 1000, jitter = 500)
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 30000)
    @CircuitBreakerName("tool-execution-post")
    public ToolExecutionResult executePost(Tool tool, Map<String, Object> body) {
        LOG.infof("Executing POST request for tool: %s", tool.name);

        long startTime = System.currentTimeMillis();

        try {
            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(tool.endpoint))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

            addAuthenticationHeaders(requestBuilder, tool);

            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long executionTime = System.currentTimeMillis() - startTime;

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                Object result = parseResponse(response.body());
                return ToolExecutionResult.success(result, executionTime);
            } else {
                return ToolExecutionResult.failure(
                        "POST request failed with status: " + response.statusCode(),
                        executionTime);
            }

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            LOG.errorf(e, "Failed to execute POST request for tool: %s", tool.name);
            return ToolExecutionResult.failure(
                    "POST request error: " + e.getMessage(),
                    executionTime);
        }
    }

    // Private helper methods

    private HttpRequest buildHttpRequest(Tool tool, Map<String, Object> params) throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .timeout(Duration.ofSeconds(10));

        // Add authentication headers
        addAuthenticationHeaders(requestBuilder, tool);

        // Determine HTTP method based on tool type and parameters
        if (params != null && !params.isEmpty()) {
            // POST request with JSON body
            String jsonBody = objectMapper.writeValueAsString(params);
            requestBuilder
                    .uri(URI.create(tool.endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        } else {
            // GET request
            requestBuilder
                    .uri(URI.create(tool.endpoint))
                    .GET();
        }

        return requestBuilder.build();
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
                        // Support both Bearer and custom header formats
                        requestBuilder.header("Authorization", "Bearer " + authConfig.apiKey);
                        requestBuilder.header("X-API-Key", authConfig.apiKey);
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
                    // For OAuth2, we would need to implement token refresh logic
                    // For now, assume the token is stored in apiKey field
                    if (authConfig.apiKey != null) {
                        requestBuilder.header("Authorization", "Bearer " + authConfig.apiKey);
                    }
                    LOG.warn("OAuth2 token refresh not implemented - using stored token");
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

    private String buildUrlWithQueryParams(String baseUrl, Map<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return baseUrl;
        }

        StringBuilder url = new StringBuilder(baseUrl);
        if (!baseUrl.contains("?")) {
            url.append("?");
        } else if (!baseUrl.endsWith("&")) {
            url.append("&");
        }

        queryParams.forEach((key, value) -> {
            url.append(key).append("=").append(value).append("&");
        });

        // Remove trailing &
        String result = url.toString();
        return result.endsWith("&") ? result.substring(0, result.length() - 1) : result;
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
