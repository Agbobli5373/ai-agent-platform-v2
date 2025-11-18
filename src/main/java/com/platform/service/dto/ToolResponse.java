package com.platform.service.dto;

import com.platform.domain.Tool;

import java.time.LocalDateTime;
import java.util.UUID;

public class ToolResponse {
    public UUID id;
    public String name;
    public String description;
    public String type;
    public String endpoint;
    public UUID ownerId;
    public LocalDateTime createdAt;

    public ToolResponse() {
    }

    public ToolResponse(Tool tool) {
        this.id = tool.id;
        this.name = tool.name;
        this.description = tool.description;
        this.type = tool.type != null ? tool.type.name() : null;
        this.endpoint = tool.endpoint;
        this.ownerId = tool.owner != null ? tool.owner.id : null;
        this.createdAt = tool.createdAt;
    }

    public static ToolResponse from(Tool tool) {
        return new ToolResponse(tool);
    }
}
