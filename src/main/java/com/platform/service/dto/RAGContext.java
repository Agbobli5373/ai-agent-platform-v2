package com.platform.service.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * RAG (Retrieval-Augmented Generation) context for agent responses.
 * Contains retrieved document passages and citation information.
 */
public class RAGContext {

    public List<RetrievedPassage> passages = new ArrayList<>();
    public boolean hasContext;
    public int totalPassages;

    public RAGContext() {
        this.hasContext = false;
        this.totalPassages = 0;
    }

    /**
     * Add a retrieved passage to the context.
     */
    public void addPassage(RetrievedPassage passage) {
        this.passages.add(passage);
        this.hasContext = true;
        this.totalPassages++;
    }

    /**
     * Format the context for injection into the prompt.
     */
    public String formatForPrompt() {
        if (!hasContext || passages.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Relevant information from documents:\n\n");

        for (int i = 0; i < passages.size(); i++) {
            RetrievedPassage passage = passages.get(i);
            sb.append(String.format("[Source %d: %s]\n", i + 1, passage.documentName));
            sb.append(passage.content);
            sb.append("\n\n");
        }

        sb.append("Please use the above information to answer the user's question. ");
        sb.append("Cite sources using [Source N] notation when referencing information.\n\n");

        return sb.toString();
    }

    /**
     * Get citations for the response.
     */
    public List<Citation> getCitations() {
        List<Citation> citations = new ArrayList<>();
        for (int i = 0; i < passages.size(); i++) {
            RetrievedPassage passage = passages.get(i);
            Citation citation = new Citation();
            citation.sourceNumber = i + 1;
            citation.documentId = passage.documentId;
            citation.documentName = passage.documentName;
            citation.chunkIndex = passage.chunkIndex;
            citation.relevanceScore = passage.relevanceScore;
            citations.add(citation);
        }
        return citations;
    }

    /**
     * A retrieved passage from a document.
     */
    public static class RetrievedPassage {
        public UUID documentId;
        public String documentName;
        public String content;
        public int chunkIndex;
        public double relevanceScore;

        public RetrievedPassage(UUID documentId, String documentName, String content,
                int chunkIndex, double relevanceScore) {
            this.documentId = documentId;
            this.documentName = documentName;
            this.content = content;
            this.chunkIndex = chunkIndex;
            this.relevanceScore = relevanceScore;
        }
    }

    /**
     * Citation information for source tracking.
     */
    public static class Citation {
        public int sourceNumber;
        public UUID documentId;
        public String documentName;
        public int chunkIndex;
        public double relevanceScore;
    }
}
