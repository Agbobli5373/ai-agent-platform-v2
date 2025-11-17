package com.platform.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AgentConfiguration {

    @NotBlank(message = "Agent name is required")
    @Size(min = 3, max = 255, message = "Agent name must be between 3 and 255 characters")
    public String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    public String description;

    @NotBlank(message = "System prompt is required")
    @Size(min = 10, max = 10000, message = "System prompt must be between 10 and 10000 characters")
    public String systemPrompt;

    @NotBlank(message = "Model name is required")
    public String modelName;

    public List<UUID> toolIds;

    public AgentConfiguration() {
        this.toolIds = new ArrayList<>();
        this.modelName = "mistral-large-latest";
    }
}
