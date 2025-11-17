package com.platform.service.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class SaveStepRequest {

    @NotNull(message = "Step is required")
    public AgentWizardSession.WizardStep step;

    @NotNull(message = "Step data is required")
    public Map<String, Object> data;
}
