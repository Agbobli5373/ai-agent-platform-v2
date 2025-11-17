package com.platform.domain.dto;

/**
 * Agent-specific configuration settings
 */
public class AgentConfiguration {
    public Double temperature;
    public Integer maxTokens;
    public Integer topK;
    public Double topP;
    public Boolean enableStreaming;
    public Integer timeoutSeconds;

    public AgentConfiguration() {
    }

    public AgentConfiguration(Double temperature, Integer maxTokens, Integer topK,
            Double topP, Boolean enableStreaming, Integer timeoutSeconds) {
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.topK = topK;
        this.topP = topP;
        this.enableStreaming = enableStreaming;
        this.timeoutSeconds = timeoutSeconds;
    }
}
