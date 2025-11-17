package com.platform.service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class DeployRequest {

    @NotNull(message = "Configuration is required")
    @Valid
    public AgentConfiguration config;

    // Optional: session ID to clean up after deployment
    public UUID sessionId;
}
