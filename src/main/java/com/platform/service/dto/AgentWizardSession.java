package com.platform.service.dto;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AgentWizardSession {

    public UUID sessionId;
    public UUID userId;
    public WizardStep currentStep;
    public Map<String, Object> stepData;
    public LocalDateTime createdAt;
    public LocalDateTime expiresAt;

    public AgentWizardSession() {
        this.sessionId = UUID.randomUUID();
        this.currentStep = WizardStep.PURPOSE;
        this.stepData = new HashMap<>();
        this.createdAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusHours(2);
    }

    public AgentWizardSession(UUID userId) {
        this();
        this.userId = userId;
    }

    public enum WizardStep {
        PURPOSE,
        PROMPT,
        TOOLS,
        PREVIEW
    }
}
