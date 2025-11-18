package com.platform.security;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.UUID;

@RequestScoped
public class SecurityContext {

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    JsonWebToken jwt;

    public UUID getCurrentUserId() {
        if (securityIdentity.isAnonymous()) {
            return null;
        }
        String userId = jwt.getSubject();
        return userId != null ? UUID.fromString(userId) : null;
    }

    public UUID getCurrentOrganizationId() {
        if (securityIdentity.isAnonymous()) {
            return null;
        }
        String orgId = jwt.getClaim("organizationId");
        return orgId != null ? UUID.fromString(orgId) : null;
    }

    public String getCurrentUserEmail() {
        if (securityIdentity.isAnonymous()) {
            return null;
        }
        return securityIdentity.getPrincipal().getName();
    }

    public boolean hasRole(String role) {
        return securityIdentity.hasRole(role);
    }
}
