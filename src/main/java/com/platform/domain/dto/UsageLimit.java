package com.platform.domain.dto;

/**
 * Usage limits configuration for an organization
 */
public class UsageLimit {
    public Long maxApiCalls;
    public Long maxTokens;
    public Long maxStorageBytes;
    public Long maxAgents;
    public Long maxDocuments;

    public UsageLimit() {
    }

    public UsageLimit(Long maxApiCalls, Long maxTokens, Long maxStorageBytes, Long maxAgents, Long maxDocuments) {
        this.maxApiCalls = maxApiCalls;
        this.maxTokens = maxTokens;
        this.maxStorageBytes = maxStorageBytes;
        this.maxAgents = maxAgents;
        this.maxDocuments = maxDocuments;
    }
}
