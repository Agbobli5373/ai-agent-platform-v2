package com.platform.service.dto;

import com.platform.domain.Tool;

import java.util.Map;

/**
 * Request DTO for tool registration.
 */
public class ToolRegistrationRequest {

    public String name;
    public String description;
    public Tool.ToolType type;
    public String endpoint;
    public AuthenticationConfig authConfig;
    public Map<String, ParameterDefinition> parameters;

    public static class AuthenticationConfig {
        public AuthType type;
        public String apiKey;
        public String username;
        public String password;
        public String oauthTokenUrl;
        public String oauthClientId;
        public String oauthClientSecret;

        public enum AuthType {
            NONE,
            API_KEY,
            BASIC_AUTH,
            OAUTH2
        }
    }

    public static class ParameterDefinition {
        public String name;
        public String type;
        public String description;
        public boolean required;
        public Object defaultValue;
    }
}
