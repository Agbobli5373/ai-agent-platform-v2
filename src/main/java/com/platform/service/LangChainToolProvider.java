package com.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.domain.Tool;
import com.platform.repository.ToolRepository;
import com.platform.service.dto.ToolExecutionResult;
import dev.langchain4j.agent.tool.P;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Provides LangChain4j @Tool annotated methods for agent tool execution.
 * This class bridges the gap between LangChain4j's tool system and our custom tool registry.
 */
@ApplicationScoped
public class LangChainToolProvider {

    private static final Logger LOG = Logger.getLogger(LangChainToolProvider.class);

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
     * @param toolId the UUID of the tool to execute
     * @param parameters JSON string containing the parameters for the tool
     * @return the result of the tool execution as a JSON string
     */
    @dev.langchain4j.agent.tool.Tool("Execute a registered tool by its ID with the given parameters")
    public String executeTool(
            @P("The UUID of the tool to execute") String toolId,
            @P("JSON string containing the parameters for the tool") String parameters) {

        LOG.infof("LangChain4j tool execution requested for tool: %s", toolId);

        try {
            // Parse tool ID
            UUID uuid = UUID.fromString(toolId);

            // Find the tool
            Tool tool = toolRepository.findByIdOptional(uuid)
                    .orElseThrow(() -> new IllegalArgumentException("Tool not found: " + toolId));

            // Parse parameters
            Map<String, Object> params = parseParameters(parameters);

            // Execute the tool
            ToolExecutionResult result = toolExecutor.execute(tool, params);

            // Format result for LLM consumption
            return formatResultForLLM(result);

        } catch (Exception e) {
            LOG.errorf(e, "Error executing tool via LangChain4j: %s", toolId);
            return formatError(e);
        }
    }

    /**
     * List all available tools for the current agent.
     * This helps the LLM understand what tools are available.
     *
     * @param organizationId the organization ID to filter tools
     * @return JSON string listing available tools
     */
    @dev.langchain4j.agent.tool.Tool("List all available tools that can be executed")
    public String listAvailableTools(@P("The organization ID") String organizationId) {
        LOG.infof("Listing available tools for organization: %s", organizationId);

        try {
            UUID orgId = UUID.fromString(organizationId);
            List<Tool> tools = toolRepository.findByOrganization(orgId);

            StringBuilder result = new StringBuilder("Available tools:\n");
            for (Tool tool : tools) {
                result.append(String.format("- %s (ID: %s): %s\n",
                        tool.name,
                        tool.id,
                        tool.description != null ? tool.description : "No description"));
            }

            return result.toString();

        } catch (Exception e) {
            LOG.errorf(e, "Error listing tools: %s", organizationId);
            return "Error listing tools: " + e.getMessage();
        }
    }

    /**
     * Get detailed information about a specific tool.
     *
     * @param toolId the tool ID
     * @return JSON string with tool details
     */
    @dev.langchain4j.agent.tool.Tool("Get detailed information about a specific tool")
    public String getToolInfo(@P("The UUID of the tool") String toolId) {
        LOG.infof("Getting tool info for: %s", toolId);

        try {
            UUID uuid = UUID.fromString(toolId);
            Tool tool = toolRepository.findByIdOptional(uuid)
                    .orElseThrow(() -> new IllegalArgumentException("Tool not found: " + toolId));

            Map<String, Object> info = Map.of(
                    "id", tool.id.toString(),
                    "name", tool.name,
                    "description", tool.description != null ? tool.description : "No description",
                    "type", tool.type.toString(),
                    "endpoint", tool.endpoint
            );

            return objectMapper.writeValueAsString(info);

        } catch (Exception e) {
            LOG.errorf(e, "Error getting tool info: %s", toolId);
            return formatError(e);
        }
    }

    // Private helper methods

    private Map<String, Object> parseParameters(String parameters) {
        if (parameters == null || parameters.trim().isEmpty() || parameters.equals("{}")) {
            return Map.of();
        }

        try {
            return objectMapper.readValue(parameters, Map.class);
        } catch (JsonProcessingException e) {
            LOG.warnf("Failed to parse parameters as JSON: %s", parameters);
            // Try to treat as a simple key-value pair
            return Map.of("input", parameters);
        }
    }

    private String formatResultForLLM(ToolExecutionResult result) {
        try {
            if (result.success) {
                Map<String, Object> response = Map.of(
                        "success", true,
                        "result", result.result != null ? result.result : "Operation completed successfully",
                        "executionTime", result.executionTimeMs + "ms"
                );
                return objectMapper.writeValueAsString(response);
            } else {
                Map<String, Object> response = Map.of(
                        "success", false,
                        "error", result.errorMessage != null ? result.errorMessage : "Unknown error",
                        "executionTime", result.executionTimeMs + "ms"
                );
                return objectMapper.writeValueAsString(response);
            }
        } catch (JsonProcessingException e) {
            LOG.errorf(e, "Failed to format result for LLM");
            return String.format("{\"success\": %s, \"result\": \"%s\"}",
                    result.success,
                    result.success ? "Success" : result.errorMessage);
        }
    }

    private String formatError(Exception e) {
        try {
            Map<String, Object> error = Map.of(
                    "success", false,
                    "error", e.getMessage() != null ? e.getMessage() : "Unknown error",
                    "type", e.getClass().getSimpleName()
            );
            return objectMapper.writeValueAsString(error);
        } catch (JsonProcessingException ex) {
            return String.format("{\"success\": false, \"error\": \"%s\"}", e.getMessage());
        }
    }
}
