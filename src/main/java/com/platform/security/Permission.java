package com.platform.security;

public enum Permission {
    // Agent permissions
    AGENT_CREATE,
    AGENT_READ,
    AGENT_UPDATE,
    AGENT_DELETE,
    AGENT_EXECUTE,

    // Tool permissions
    TOOL_CREATE,
    TOOL_READ,
    TOOL_UPDATE,
    TOOL_DELETE,

    // Document permissions
    DOCUMENT_CREATE,
    DOCUMENT_READ,
    DOCUMENT_UPDATE,
    DOCUMENT_DELETE,

    // User permissions
    USER_CREATE,
    USER_READ,
    USER_UPDATE,
    USER_DELETE,

    // Organization permissions
    ORGANIZATION_READ,
    ORGANIZATION_UPDATE,
    ORGANIZATION_SETTINGS,

    // Monitoring permissions
    MONITORING_READ,
    MONITORING_EXPORT
}
