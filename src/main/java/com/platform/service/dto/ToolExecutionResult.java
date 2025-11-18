package com.platform.service.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Result of tool execution.
 */
public class ToolExecutionResult {

    public boolean success;
    public Object result;
    public String errorMessage;
    public long executionTimeMs;
    public LocalDateTime timestamp;
    public Map<String, Object> metadata;

    public ToolExecutionResult() {
        this.timestamp = LocalDateTime.now();
    }

    public static ToolExecutionResult success(Object result, long executionTimeMs) {
        ToolExecutionResult executionResult = new ToolExecutionResult();
        executionResult.success = true;
        executionResult.result = result;
        executionResult.executionTimeMs = executionTimeMs;
        return executionResult;
    }

    public static ToolExecutionResult failure(String errorMessage, long executionTimeMs) {
        ToolExecutionResult executionResult = new ToolExecutionResult();
        executionResult.success = false;
        executionResult.errorMessage = errorMessage;
        executionResult.executionTimeMs = executionTimeMs;
        return executionResult;
    }
}
