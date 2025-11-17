package com.platform.security;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class RBACPolicy {

    private static final Map<Role, Set<Permission>> ROLE_PERMISSIONS = Map.of(
            Role.ADMIN, EnumSet.allOf(Permission.class),

            Role.USER, EnumSet.of(
                    Permission.AGENT_CREATE,
                    Permission.AGENT_READ,
                    Permission.AGENT_UPDATE,
                    Permission.AGENT_DELETE,
                    Permission.AGENT_EXECUTE,
                    Permission.TOOL_CREATE,
                    Permission.TOOL_READ,
                    Permission.TOOL_UPDATE,
                    Permission.TOOL_DELETE,
                    Permission.DOCUMENT_CREATE,
                    Permission.DOCUMENT_READ,
                    Permission.DOCUMENT_UPDATE,
                    Permission.DOCUMENT_DELETE,
                    Permission.MONITORING_READ,
                    Permission.ORGANIZATION_READ),

            Role.VIEWER, EnumSet.of(
                    Permission.AGENT_READ,
                    Permission.AGENT_EXECUTE,
                    Permission.TOOL_READ,
                    Permission.DOCUMENT_READ,
                    Permission.MONITORING_READ,
                    Permission.ORGANIZATION_READ));

    /**
     * Check if a role has a specific permission
     */
    public boolean hasPermission(Role role, Permission permission) {
        Set<Permission> permissions = ROLE_PERMISSIONS.get(role);
        return permissions != null && permissions.contains(permission);
    }

    /**
     * Check if a role has a specific permission (string-based)
     */
    public boolean hasPermission(String roleStr, Permission permission) {
        try {
            Role role = Role.fromString(roleStr);
            return hasPermission(role, permission);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Get all permissions for a role
     */
    public Set<Permission> getPermissions(Role role) {
        return ROLE_PERMISSIONS.getOrDefault(role, EnumSet.noneOf(Permission.class));
    }

    /**
     * Check if a role has any of the specified permissions
     */
    public boolean hasAnyPermission(Role role, Permission... permissions) {
        Set<Permission> rolePermissions = ROLE_PERMISSIONS.get(role);
        if (rolePermissions == null) {
            return false;
        }
        for (Permission permission : permissions) {
            if (rolePermissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a role has all of the specified permissions
     */
    public boolean hasAllPermissions(Role role, Permission... permissions) {
        Set<Permission> rolePermissions = ROLE_PERMISSIONS.get(role);
        if (rolePermissions == null) {
            return false;
        }
        for (Permission permission : permissions) {
            if (!rolePermissions.contains(permission)) {
                return false;
            }
        }
        return true;
    }
}
