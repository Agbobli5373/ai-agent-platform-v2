package com.platform.service;

import com.platform.domain.Agent;
import com.platform.domain.Tool;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Orchestrates tool execution for agents with retry logic,
 * timeout handling, and response aggregation.
 */
@ApplicationScoped
public class ToolExecutionOrchestrator {

    /**
     * Execute a chain of tools for an agent.
     *
     * @param agent   The agent requesting tool execution
     * @param toolIds List of tool IDs to execute
     * @param context Execution context with parameters
     * @return Aggregated results from all tool executions
     */
    public CompletionStage<ToolChainResult> executeToolChain(
            Agent agent,
            List<String> toolIds,
            Map<String, Object> context) {

        Log.infof("Executing tool chain for agent %s with %d tools", agent.id, toolIds.size());

        List<CompletableFuture<ToolExecutionResult>> futures = new ArrayList<>();

        for (String toolId : toolIds) {
            // Find the tool in agent's tools
            Tool tool = findToolById(agent, toolId);
            if (tool == null) {
                Log.warnf("Tool %s not found for agent %s", toolId, agent.id);
                continue;
            }

            // Execute a tool asynchronously with fault tolerance
            CompletableFuture<ToolExecutionResult> future = executeToolWithRetry(tool, context)
                    .toCompletableFuture();
            futures.add(future);
        }

        // Wait for all tools to complete and aggregate results
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<ToolExecutionResult> results = futures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList());

                    return aggregateResults(results);
                })
                .exceptionally(throwable -> {
                    Log.errorf(throwable, "Error executing tool chain for agent %s", agent.id);
                    return new ToolChainResult(false, "Tool chain execution failed: " + throwable.getMessage(), null);
                });
    }

    /**
     * Execute a single tool with retry and timeout logic.
     * Implements manual retry logic with exponential backoff.
     *
     * @param tool    The tool to execute
     * @param context Execution context
     * @return Tool execution result
     */
    public CompletionStage<ToolExecutionResult> executeToolWithRetry(
            Tool tool,
            Map<String, Object> context) {

        return CompletableFuture.supplyAsync(() -> {
            int maxRetries = 3;
            int retryDelay = 1000; // milliseconds
            Exception lastException = null;

            for (int attempt = 0; attempt <= maxRetries; attempt++) {
                try {
                    Log.infof("Executing tool: %s (%s) - Attempt %d/%d",
                            tool.name, tool.type, attempt + 1, maxRetries + 1);

                    // Execute with timeout
                    Object result = executeToolWithTimeout(tool, context, 10000);

                    Log.infof("Tool %s executed successfully", tool.name);

                    return new ToolExecutionResult(
                            tool.id.toString(),
                            tool.name,
                            true,
                            result,
                            null,
                            System.currentTimeMillis());

                } catch (Exception e) {
                    lastException = e;
                    Log.warnf(e, "Error executing tool %s (attempt %d/%d)",
                            tool.name, attempt + 1, maxRetries + 1);

                    if (attempt < maxRetries) {
                        try {
                            // Exponential backoff with jitter
                            long delay = retryDelay * (1L << attempt) + (long) (Math.random() * 200);
                            Thread.sleep(delay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }

            // All retries failed
            Log.errorf(lastException, "Tool %s failed after %d retries", tool.name, maxRetries);

            return new ToolExecutionResult(
                    tool.id.toString(),
                    tool.name,
                    false,
                    null,
                    lastException != null ? lastException.getMessage() : "Unknown error",
                    System.currentTimeMillis());
        });
    }

    /**
     * Execute tool with timeout.
     */
    private Object executeToolWithTimeout(Tool tool, Map<String, Object> context, long timeoutMs)
            throws Exception {
        CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
            try {
                return executeTool(tool, context);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        try {
            return future.get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            throw new Exception("Tool execution timed out after " + timeoutMs + "ms");
        } catch (java.util.concurrent.ExecutionException e) {
            throw new Exception("Tool execution failed: " + e.getCause().getMessage(), e.getCause());
        }
    }

    /**
     * Execute the actual tool based on its type.
     * This is a placeholder - actual implementation would call external APIs.
     *
     * @param tool    The tool to execute
     * @param context Execution context
     * @return Tool execution result
     */
    private Object executeTool(Tool tool, Map<String, Object> context) {
        switch (tool.type) {
            case REST_API:
                return executeRestApiTool(tool, context);
            case FUNCTION:
                return executeFunctionTool(tool, context);
            case DATABASE:
                return executeDatabaseTool(tool, context);
            default:
                throw new UnsupportedOperationException("Unsupported tool type: " + tool.type);
        }
    }

    /**
     * Execute REST API tool.
     * Placeholder implementation - would use REST client in production.
     */
    private Object executeRestApiTool(Tool tool, Map<String, Object> context) {
        Log.debugf("Executing REST API tool: %s at %s", tool.name, tool.endpoint);

        // TODO: Implement actual REST client call
        // For now, return mock response
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("endpoint", tool.endpoint);
        response.put("data", "Mock response from " + tool.name);

        return response;
    }

    /**
     * Execute function tool.
     * Placeholder implementation.
     */
    private Object executeFunctionTool(Tool tool, Map<String, Object> context) {
        Log.debugf("Executing function tool: %s", tool.name);

        // TODO: Implement function execution
        return Map.of("result", "Function executed: " + tool.name);
    }

    /**
     * Execute database tool.
     * Placeholder implementation.
     */
    private Object executeDatabaseTool(Tool tool, Map<String, Object> context) {
        Log.debugf("Executing database tool: %s", tool.name);

        // TODO: Implement database query execution
        return Map.of("result", "Database query executed: " + tool.name);
    }

    /**
     * Find a tool by ID in the agent's tool list.
     */
    private Tool findToolById(Agent agent, String toolId) {
        return agent.tools.stream()
                .map(agentTool -> agentTool.tool)
                .filter(tool -> tool.id.toString().equals(toolId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Aggregate results from multiple tool executions.
     */
    private ToolChainResult aggregateResults(List<ToolExecutionResult> results) {
        boolean allSuccessful = results.stream().allMatch(r -> r.success);

        String summary = results.stream()
                .map(r -> String.format("%s: %s", r.toolName, r.success ? "success" : "failed"))
                .collect(Collectors.joining(", "));

        Map<String, Object> aggregatedData = new HashMap<>();
        for (ToolExecutionResult result : results) {
            aggregatedData.put(result.toolId, result.result);
        }

        Log.infof("Tool chain execution completed. Success: %s, Summary: %s", allSuccessful, summary);

        return new ToolChainResult(allSuccessful, summary, aggregatedData);
}

/**
 * Result of a single tool execution.
 */
public static class ToolExecutionResult {
public String toolId;
public String toolName;
public boolean success;
public Object result;
public String error;
public long executionTimeMs;

public ToolExecutionResult(String toolId, String toolName, boolean success,
                                   Object result, String error, long executionTimeMs) {
    this.toolId = toolId;
            this.toolName = toolName;
            this.success = success;
            this.result = result;
            this.error = error;
            this.executionTimeMs = executionTimeMs;
        }
    }

    /**
     * Aggregated result of tool chain execution.
     */
    public static class ToolChainResult {
        public boolean success;
        public String summary;
        public Map<String, Object> results;

        public ToolChainResult(boolean success, String summary, Map<String, Object> results) {
            this.success = success;
            this.summary = summary;
            this.results = results;
        }
    }
}
