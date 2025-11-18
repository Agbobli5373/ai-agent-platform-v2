package com.platform.service.dto;

/**
 * Result of tool validation and connectivity check.
 */
public class ToolValidationResult {

    public boolean valid;
    public String message;
    public long responseTimeMs;
    public Integer statusCode;
    public String errorDetails;

    public ToolValidationResult() {
    }

    public ToolValidationResult(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }

    public static ToolValidationResult success(String message, long responseTimeMs, Integer statusCode) {
        ToolValidationResult result = new ToolValidationResult(true, message);
        result.responseTimeMs = responseTimeMs;
        result.statusCode = statusCode;
        return result;
    }

    public static ToolValidationResult failure(String message, String errorDetails) {
        ToolValidationResult result = new ToolValidationResult(false, message);
        result.errorDetails = errorDetails;
        return result;
    }
}
