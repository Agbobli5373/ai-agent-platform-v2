package com.platform.service;

import com.platform.domain.Agent;
import com.platform.domain.Tool;
import com.platform.service.dto.ToolExecutionResult;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@ApplicationScoped
public class ToolExecutionOrchestrator {

    @Inject
    ToolExecutor toolExecutor;

    public CompletionStage<ToolChainResult> executeToolChain(Agent agent, List<String> toolIds, Map<String, Object> context) {
        Log.infof("Executing tool chain for agent %s with %d tools", agent.id, toolIds.size());
        List<CompletableFuture<ToolExecutionResult>> futures = new ArrayList<>();

        for (String toolId : toolIds) {
            Tool tool = findToolById(agent, toolId);
            if (tool == null) {
                Log.warnf("Tool %s not found for agent %s", toolId, agent.id);
                continue;
            }
            CompletableFuture<ToolExecutionResult> future = executeToolWithRetry(tool, context).toCompletableFuture();
            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<ToolExecutionResult> results = futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
                    return aggregateResults(results);
                })
                .exceptionally(throwable -> {
                    Log.errorf(throwable, "Error executing tool chain for agent %s", agent.id);
                    return new ToolChainResult(false, "Tool chain execution failed: " + throwable.getMessage(), null);
                });
    }

    public CompletionStage<ToolExecutionResult> executeToolWithRetry(Tool tool, Map<String, Object> context) {
        return CompletableFuture.supplyAsync(() -> {
            Log.infof("Executing tool with retry: %s (%s)", tool.name, tool.type);
            return toolExecutor.execute(tool, context);
        });
    }

    private Tool findToolById(Agent agent, String toolId) {
        return agent.tools.stream()
                .map(agentTool -> agentTool.tool)
                .filter(tool -> tool.id.toString().equals(toolId))
                .findFirst()
                .orElse(null);
    }

    private ToolChainResult aggregateResults(List<ToolExecutionResult> results) {
        boolean allSuccessful = results.stream().allMatch(r -> r.success);
        String summary = results.stream()
                .map(r -> String.format("Tool execution: %s", r.success ? "success" : "failed"))
                .collect(Collectors.joining(", "));
        Map<String, Object> aggregatedData = new HashMap<>();
        int index = 0;
        for (ToolExecutionResult result : results) {
            aggregatedData.put("result_" + index++, result.result);
        }
        Log.infof("Tool chain execution completed. Success: %s, Summary: %s", allSuccessful, summary);
        return new ToolChainResult(allSuccessful, summary, aggregatedData);
    }

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
