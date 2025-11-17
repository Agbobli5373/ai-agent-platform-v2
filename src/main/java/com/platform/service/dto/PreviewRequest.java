package com.platform.service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PreviewRequest {

    @NotNull(message = "Configuration is required")
    @Valid
    public AgentConfiguration config;

    @NotBlank(message = "Test prompt is required")
    public String testPrompt;
}
