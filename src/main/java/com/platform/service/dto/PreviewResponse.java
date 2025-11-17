package com.platform.service.dto;

public class PreviewResponse {

    public String response;
    public boolean success;
    public String error;
    public long responseTimeMs;

    public PreviewResponse() {
    }

    public PreviewResponse(String response, long responseTimeMs) {
        this.response = response;
        this.success = true;
        this.responseTimeMs = responseTimeMs;
    }

    public PreviewResponse(String error) {
        this.success = false;
        this.error = error;
    }
}
