package com.platform.security;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class SecurityContext {

    @Inject
    SecurityIdentity securityIdentity;

    public Long getCurrentUserId() {
        if (securityIdentity.isAnonymous()) {
            return null;
        }
        String userId = securityIdentity.getAttribute("userId");
        return userId != null ? Long.parseLong(userId) : null;
    }

    public Long getCurrentOrganizationId() {
        if (securityIdentity.isAnonymous()) {
            return null;
        }
        String orgId = securityIdentity.getAttribute("organizationId");
        return orgId != null ? Long.parseLong(orgId) : null;
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
