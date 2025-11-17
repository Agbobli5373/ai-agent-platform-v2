package com.platform.security.dto;

import java.util.UUID;

public class AuthenticationResponse {

    public String accessToken;
    public String refreshToken;
    public String tokenType = "Bearer";
    public Long expiresIn;
    public UserInfo user;

    public AuthenticationResponse() {
    }

    public AuthenticationResponse(String accessToken, String refreshToken, Long expiresIn, UserInfo user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.user = user;
    }

    public static class UserInfo {
        public UUID id;
        public String email;
        public String role;
        public UUID organizationId;

        public UserInfo() {
        }

        public UserInfo(UUID id, String email, String role, UUID organizationId) {
            this.id = id;
            this.email = email;
            this.role = role;
            this.organizationId = organizationId;
        }
    }
}
