package com.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.domain.Tool;
import com.platform.repository.ToolRepository;
import com.platform.service.dto.ToolExecutionResult;
import dev.langchain4j.agent.tool.P;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides LangChain4j @Tool annotated methods for agent tool execution.
 * This class bridges the gap between LangChain4j's tool system and our custom
 * tool registry.
 * 
 * Key responsibilities:
 * - Define @Tool annotated methods for LangChain4j agent integration
 * - Extract and parse tool parameters from LLM responses
 * - Format tool execution results for LLM consumption
 * - Log all tool executions for audit and debugging
 * 
 * Requirements: 3.4, 3.5
 */
@ApplicationScoped
public class LangChainToolProvider {

    private static final Logger LOG = Logger.getLogger(LangChainToolProvider.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Inject
    ToolExecutor toolExecutor;

    @Inject
    ToolRepository toolRepository;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Execute a registered tool by ID.
     * This method is exposed to LangChain4j as a tool that agents can call.
     * 
     * Implements Requirements 3.4, 3.5:
     * - Tool execution with parameter mapping
     * - Tool execution logging
     *
     * @param toolId     the UUID of the tool to execute
     * @param parameters JSON string containing the parameters for the tool
     * @return the result of the tool execution as a JSON string
     */
    @dev.langchain4j.agent.tool.Tool("Execute a registered tool by its ID with the given parameters. " +
            "Returns a JSON object with success status, result data, and execution metadata.")
    public String executeTool(
            @P("The UUID of the tool to execute") String toolId,
            @P("JSON string containing the parameters for the tool, e.g., {\"param1\": \"value1\", \"param2\": \"value2\"}") String parameters) {

        LocalDateTime startTime = LocalDateTime.now();
        LOG.infof("[TOOL EXECUTION START] Tool ID: %s, Parameters: %s, Timestamp: %s",
                toolId, parameters, startTime.format(TIMESTAMP_FORMATTER));

        try {
            // Parse tool ID
            UUID uuid = parseToolId(toolId);

            // Find the tool
            Tool tool = toolRepository.findByIdOptional(uuid)
                    .orElseThrow(() -> new IllegalArgumentException("Tool not found: " + toolId));

            LOG.infof("[TOOL FOUND] Name: %s, Type: %s, Endpoint: %s", tool.name, tool.type, tool.endpoint);

            // Parse and validate parameters
            Map<String, Object> params = parseParameters(parameters);
            LOG.infof("[PARAMETERS EXTRACTED] Parsed %d parameters from LLM response", params.size());

            // Execute the tool
            ToolExecutionResult result = toolExecutor.execute(tool, params);

            // Log execution result
            logToolExecution(tool, params, result, startTime);

            // Format result for LLM consumption
            String formattedResult = formatResultForLLM(tool, result, startTime);
            LOG.infof("[TOOL EXECUTION END] Tool: %s, Success: %s, Duration: %dms",
                    tool.name, result.success, result.executionTimeMs);

            return formattedResult;

        } catch (IllegalArgumentException e) {
            LOG.errorf(e, "[TOOL EXECUTION ERROR] Invalid tool ID or parameters: %s", toolId);
            return formatError("Invalid input", e.getMessage(), startTime);
        } catch (Exception e) {
            LOG.errorf(e, "[TOOL EXECUTION ERROR] Unexpected error executing tool: %s", toolId);
            return formatError("Execution failed", e.getMessage(), startTime);
        }
    }

    /**
     * List all available tools for the current agent.
     * This helps the LLM understand what tools are available.
     *
     * @param organizationId the organization ID to filter tools
     * @return JSON string listing available tools
     */
    @dev.langchain4j.agent.tool.Tool("List all available tools that can be executed. " +
            "Returns a formatted list of tools with their IDs, names, and descriptions.")
    public String listAvailableTools(@P("The organization ID") String organizationId) {
        LOG.infof("[LIST TOOLS] Organization: %s", organizationId);

        try {
            UUID orgId = UUID.fromString(organizationId);
            List<Tool> tools = toolRepository.findByOrganization(orgId);

            LOG.infof("[TOOLS FOUND] Count: %d for organization: %s", tools.size(), organizationId);

            // Format tools for LLM consumption
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("count", tools.size());
            response.put("organizationId", organizationId);

            List<Map<String, Object>> toolList = new ArrayList<>();
            for (Tool tool : tools) {
                Map<String, Object> toolInfo = new LinkedHashMap<>();
                toolInfo.put("id", tool.id.toString());
                toolInfo.put("name", tool.name);
                toolInfo.put("description", tool.description != null ? tool.description : "No description");
                toolInfo.put("type", tool.type.toString());
                toolInfo.put("endpoint", tool.endpoint);
                toolList.add(toolInfo);
            }
            response.put("tools", toolList);

            return objectMapper.writeValueAsString(response);

        } catch (IllegalArgumentException e) {
            LOG.errorf(e, "[LIST TOOLS ERROR] Invalid organization ID: %s", organizationId);
            return formatError("Invalid organization ID", e.getMessage(), LocalDateTime.now());
        } catch (JsonProcessingException e) {
            LOG.errorf(e, "[LIST TOOLS ERROR] Failed to format response");
            return formatError("Response formatting failed", e.getMessage(), LocalDateTime.now());
        }
    }

    /**
     * Get detailed information about a specific tool.
     *
     * @param toolId the tool ID
     * @return JSON string with tool details
     */
    @dev.langchain4j.agent.tool.Tool("Get detailed information about a specific tool including its configuration and parameters. "
            +
            "Returns a JSON object with complete tool metadata.")
    public String getToolInfo(@P("The UUID of the tool") String toolId) {
        LOG.infof("[GET TOOL INFO] Tool ID: %s", toolId);

        try {
            UUID uuid = parseToolId(toolId);
            Tool tool = toolRepository.findByIdOptional(uuid)
                    .orElseThrow(() -> new IllegalArgumentException("Tool not found: " + toolId));

            LOG.infof("[TOOL INFO] Name: %s, Type: %s", tool.name, tool.type);

            Map<String, Object> info = new LinkedHashMap<>();
            info.put("success", true);
            info.put("id", tool.id.toString());
            info.put("name", tool.name);
            info.put("description", tool.description != null ? tool.description : "No description");
            info.put("type", tool.type.toString());
            info.put("endpoint", tool.endpoint);
            info.put("createdAt", tool.createdAt.format(TIMESTAMP_FORMATTER));

            // Include parameter schema if available
            if (tool.parameters != null && !tool.parameters.isEmpty()) {
                try {
                    Object paramSchema = objectMapper.readValue(tool.parameters, Object.class);
                    info.put("parameters", paramSchema);
                } catch (JsonProcessingException e) {
                    LOG.warnf("Failed to parse parameter schema for tool %s", toolId);
                    info.put("parameters", "Unable to parse parameter schema");
                }
            }

            return objectMapper.writeValueAsString(info);

        } catch (IllegalArgumentException e) {
            LOG.errorf(e, "[GET TOOL INFO ERROR] Invalid tool ID: %s", toolId);
            return formatError("Invalid tool ID", e.getMessage(), LocalDateTime.now());
        } catch (JsonProcessingException e) {
            LOG.errorf(e, "[GET TOOL INFO ERROR] Failed to format response");
            return formatError("Response formatting failed", e.getMessage(), LocalDateTime.now());
        }
    }

    // ==================== Parameter Extraction Methods ====================

    /**
     * Parse tool ID from string, handling various formats.
     * Extracts UUID from LLM responses that may include extra text.
     *
     * @param toolId the tool ID string from LLM
     * @return parsed UUID
     * @throws IllegalArgumentException if UUID cannot be parsed
     */
    private UUID parseToolId(String toolId) {
        if (toolId == null || toolId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tool ID cannot be null or empty");
        }

        // Clean up the tool ID - remove quotes, whitespace, etc.
        String cleaned = toolId.trim().replaceAll("[\"']", "");

        // Try to extract UUID pattern if embedded in text
        String uuidPattern = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(uuidPattern);
        java.util.regex.Matcher matcher = pattern.matcher(cleaned);

        if (matcher.find()) {
            cleaned = matcher.group();
        }

        try {
            return UUID.fromString(cleaned);
        } catch (IllegalArgumentException e) {
            LOG.warnf("Failed to parse tool ID: %s", toolId);
            throw new IllegalArgumentException("Invalid tool ID format: " + toolId, e);
        }
    }

    /**
     * Parse parameters from LLM response.
     * Handles various formats: JSON objects, JSON strings, plain text.
     * 
     * Implements Requirement 3.4: Tool parameter extraction from LLM responses
     *
     * @param parameters the parameters string from LLM
     * @return parsed parameter map
     */
    private Map<String, Object> parseParameters(String parameters) {
        if (parameters == null || parameters.trim().isEmpty() || parameters.equals("{}")
                || parameters.equalsIgnoreCase("null")) {
            LOG.debug("No parameters provided, returning empty map");
            return new HashMap<>();
        }

        String cleaned = parameters.trim();

        // Try parsing as JSON object
        try {
            return objectMapper.readValue(cleaned, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            LOG.debugf("Failed to parse as JSON object: %s", cleaned);
        }

        // Try parsing as JSON string (double-encoded)
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            try {
                String unescaped = objectMapper.readValue(cleaned, String.class);
                return objectMapper.readValue(unescaped, new TypeReference<Map<String, Object>>() {
                });
            } catch (JsonProcessingException e) {
                LOG.debugf("Failed to parse as double-encoded JSON: %s", cleaned);
            }
        }

        // If all else fails, treat as a single "input" parameter
        LOG.infof("Treating parameters as plain text input: %s", cleaned);
        Map<String, Object> result = new HashMap<>();
        result.put("input", cleaned);
        return result;
    }

    // ==================== Result Formatting Methods ====================

    /**
     * Format tool execution result for LLM consumption.
     * Creates a structured JSON response that the LLM can easily parse and
     * understand.
     * 
     * Implements Requirement 3.4: Tool result formatting for LLM consumption
     *
     * @param tool      the executed tool
     * @param result    the execution result
     * @param startTime execution start time
     * @return formatted JSON string
     */
    private String formatResultForLLM(Tool tool, ToolExecutionResult result, LocalDateTime startTime) {
        try {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", result.success);
            response.put("toolName", tool.name);
            response.put("toolId", tool.id.toString());
            response.put("timestamp", startTime.format(TIMESTAMP_FORMATTER));
            response.put("executionTimeMs", result.executionTimeMs);

            if (result.success) {
                response.put("result", result.result != null ? result.result : "Operation completed successfully");
                response.put("message", "Tool executed successfully");
            } else {
                response.put("error", result.errorMessage != null ? result.errorMessage : "Unknown error");
                response.put("message", "Tool execution failed");
            }

            // Include metadata if available
            if (result.metadata != null && !result.metadata.isEmpty()) {
                response.put("metadata", result.metadata);
            }

            return objectMapper.writeValueAsString(response);

        } catch (JsonProcessingException e) {
            LOG.errorf(e, "Failed to format result for LLM");
            // Fallback to simple format
            return String.format("{\"success\": %s, \"result\": \"%s\", \"toolName\": \"%s\"}",
                    result.success,
                    result.success ? "Success" : result.errorMessage,
                    tool.name);
        }
    }

    /**
     * Format error response for LLM consumption.
     *
     * @param errorType    the type of error
     * @param errorMessage the error message
     * @param timestamp    when the error occurred
     * @return formatted JSON error string
     */
    private String formatError(String errorType, String errorMessage, LocalDateTime timestamp) {
        try {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("errorType", errorType);
            error.put("error", errorMessage != null ? errorMessage : "Unknown error");
            error.put("timestamp", timestamp.format(TIMESTAMP_FORMATTER));
            error.put("message", "Tool execution failed");

            return objectMapper.writeValueAsString(error);
        } catch (JsonProcessingException e) {
            LOG.errorf(e, "Failed to format error response");
            return String.format("{\"success\": false, \"error\": \"%s: %s\"}", errorType, errorMessage);
        }
    }

    // ==================== Logging Methods ====================

    /**
     * Log tool execution for audit and debugging purposes.
     * 
     * Implements Requirement 3.5: Tool execution logging
     *
     * @param tool      the executed tool
     * @param params    the parameters used
     * @param result    the execution result
     * @param startTime execution start time
     */
    private void logToolExecution(Tool tool, Map<String, Object> params, ToolExecutionResult result,
            LocalDateTime startTime) {
        LocalDateTime endTime = LocalDateTime.now();

        Map<String, Object> logEntry = new LinkedHashMap<>();
        logEntry.put("timestamp", startTime.format(TIMESTAMP_FORMATTER));
        logEntry.put("toolId", tool.id.toString());
        logEntry.put("toolName", tool.name);
        logEntry.put("toolType", tool.type.toString());
        logEntry.put("endpoint", tool.endpoint);
        logEntry.put("parameterCount", params.size());
        logEntry.put("success", result.success);
        logEntry.put("executionTimeMs", result.executionTimeMs);
        logEntry.put("endTime", endTime.format(TIMESTAMP_FORMATTER));

        if (!result.success && result.errorMessage != null) {
            logEntry.put("errorMessage", result.errorMessage);
        }

        try {
            String logJson = objectMapper.writeValueAsString(logEntry);
            LOG.infof("[TOOL EXECUTION LOG] %s", logJson);
        } catch (JsonProcessingException e) {
            LOG.warnf("Failed to serialize tool execution log: %s", e.getMessage());
            LOG.infof("[TOOL EXECUTION LOG] Tool: %s, Success: %s, Duration: %dms",
                    tool.name, result.success, result.executionTimeMs);
        }
    }
}
