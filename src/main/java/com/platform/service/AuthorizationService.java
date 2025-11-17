package com.platform.service;

import com.platform.exception.AuthorizationException;
import com.platform.security.Permission;
import com.platform.security.RBACPolicy;
import com.platform.security.Role;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class AuthorizationService {

    @Inject
    RBACPolicy rbacPolicy;

    @Inject
    SecurityIdentity securityIdentity;

    /**
     * Check if the current user has a specific permission
     */
    public boolean hasPermission(Permission permission) {
        if (securityIdentity.isAnonymous()) {
            return false;
        }

        String role = securityIdentity.getRoles().stream().findFirst().orElse(null);
        if (role == null) {
            return false;
        }

        return rbacPolicy.hasPermission(role, permission);
    }

    /**
     * Require the current user to have a specific permission
     */
    public void requirePermission(Permission permission) {
        if (!hasPermission(permission)) {
            throw new AuthorizationException("Insufficient permissions");
        }
    }

    /**
     * Get the current user's ID from the security context
     */
    public UUID getCurrentUserId() {
        if (securityIdentity.isAnonymous()) {
            throw new AuthorizationException("User not authenticated");
        }

        String userId = securityIdentity.getAttribute("sub");
        if (userId == null || userId.isBlank()) {
            throw new AuthorizationException("User ID not found in token");
        }

        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new AuthorizationException("Invalid user ID format in token: " + userId);
        }
    }

    /**
     * Get the current user's organization ID
     */
    public UUID getCurrentOrganizationId() {
        if (securityIdentity.isAnonymous()) {
            throw new AuthorizationException("User not authenticated");
        }

        String orgId = securityIdentity.getAttribute("organizationId");
        if (orgId == null || orgId.isBlank()) {
            throw new AuthorizationException("User has no organization");
        }

        try {
            return UUID.fromString(orgId);
        } catch (IllegalArgumentException e) {
            throw new AuthorizationException("Invalid organization ID format in token: " + orgId);
        }
    }

    /**
     * Check if a resource belongs to the current user's organization
     */
    public boolean belongsToCurrentOrganization(UUID resourceOrganizationId) {
        try {
            UUID currentOrgId = getCurrentOrganizationId();
            return currentOrgId.equals(resourceOrganizationId);
        } catch (AuthorizationException e) {
            return false;
        }
    }

    /**
     * Require a resource to belong to the current user's organization
     */
    public void requireSameOrganization(UUID resourceOrganizationId) {
        if (!belongsToCurrentOrganization(resourceOrganizationId)) {
            throw new AuthorizationException("Access denied: resource belongs to different organization");
        }
    }

    /**
     * Check if the current user is an admin
     */
    public boolean isAdmin() {
        return hasPermission(Permission.USER_CREATE) && hasPermission(Permission.USER_DELETE);
    }

    /**
     * Get the current user's role
     */
    public Role getCurrentRole() {
        if (securityIdentity.isAnonymous()) {
            throw new AuthorizationException("User not authenticated");
        }

        String role = securityIdentity.getRoles().stream().findFirst().orElse(null);
        if (role == null) {
            throw new AuthorizationException("User has no role");
        }

        return Role.fromString(role);
    }
}
