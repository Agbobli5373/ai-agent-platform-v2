package com.platform.service.dto;

/**
 * Configuration for RAG (Retrieval-Augmented Generation) capabilities.
 */
public class RAGConfiguration {
    
    /**
     * Whether RAG is enabled for this agent.
     */
    public boolean enabled = false;
    
    /**
     * Minimum relevance score threshold (0.0 to 1.0).
     * Only passages with relevance >= threshold will be included.
     */
    public double relevanceThreshold = 0.7;
    
    /**
     * Maximum number of passages to retrieve.
     */
    public int maxPassages = 5;
    
    /**
     * Whether to include citations in responses.
     */
    public boolean includeCitations = true;
    
    public RAGConfiguration() {
    }
    
    public RAGConfiguration(boolean enabled, double relevanceThreshold, int maxPassages, boolean includeCitations) {
        this.enabled = enabled;
        this.relevanceThreshold = relevanceThreshold;
        this.maxPassages = maxPassages;
        this.includeCitations = includeCitations;
    }
    
    /**
     * Create default RAG configuration.
     */
    public static RAGConfiguration createDefault() {
        return new RAGConfiguration(true, 0.7, 5, true);
    }
    
    /**
     * Validate the configuration.
     */
    public void validate() {
        if (relevanceThreshold < 0.0 || relevanceThreshold > 1.0) {
            throw new IllegalArgumentException("Relevance threshold must be between 0.0 and 1.0");
        }
        if (maxPassages < 1 || maxPassages > 20) {
            throw new IllegalArgumentException("Max passages must be between 1 and 20");
        }
    }
}
