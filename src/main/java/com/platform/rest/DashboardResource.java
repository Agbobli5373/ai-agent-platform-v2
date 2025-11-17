package com.platform.rest;

import com.platform.domain.User;
import com.platform.service.DashboardService;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@Path("/dashboard")
public class DashboardResource {

    @Inject
    @io.quarkus.qute.Location("dashboard/home.html")
    Template dashboardHome;

    @Inject
    DashboardService dashboardService;

    @GET
    @Produces(MediaType.TEXT_HTML)
    @RolesAllowed({ "USER", "ADMIN" })
    public TemplateInstance home(@Context SecurityContext securityContext) {
        // Get current user from security context
        String userEmail = securityContext.getUserPrincipal().getName();
        User user = User.find("email", userEmail).firstResult();

        // Get user initials for avatar
        String initials = getUserInitials(user);
        String userName = getUserName(user);
        String orgName = user.organization != null ? user.organization.name : "My Organization";

        return dashboardHome
                .data("userInitials", initials)
                .data("userName", userName)
                .data("userEmail", user.email)
                .data("userRole", user.role)
                .data("currentOrgName", orgName);
    }

    private String getUserInitials(User user) {
        if (user.email == null || user.email.isEmpty()) {
            return "U";
        }
        String[] parts = user.email.split("@")[0].split("\\.");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
        return user.email.substring(0, Math.min(2, user.email.length())).toUpperCase();
    }

    private String getUserName(User user) {
        if (user.email == null || user.email.isEmpty()) {
            return "User";
        }
        String localPart = user.email.split("@")[0];
        String[] parts = localPart.split("\\.");
        if (parts.length >= 2) {
            return capitalize(parts[0]) + " " + capitalize(parts[1]);
        }
        return capitalize(localPart);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    @GET
    @Path("/stats")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "USER", "ADMIN" })
    public DashboardStats getStats() {
        return dashboardService.getDashboardStats();
    }

    @GET
    @Path("/activity")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "USER", "ADMIN" })
    public java.util.List<ActivityItem> getRecentActivity() {
        return dashboardService.getRecentActivity();
    }

    public static class DashboardStats {
        public int totalAgents;
        public int totalConversations;
        public int satisfactionScore;
        public double avgResponseTime;

        public DashboardStats(int totalAgents, int totalConversations, int satisfactionScore, double avgResponseTime) {
            this.totalAgents = totalAgents;
            this.totalConversations = totalConversations;
            this.satisfactionScore = satisfactionScore;
            this.avgResponseTime = avgResponseTime;
        }
    }

    public static class ActivityItem {
        public String id;
        public String type;
        public String message;
        public String timestamp;

        public ActivityItem(String id, String type, String message, String timestamp) {
            this.id = id;
            this.type = type;
            this.message = message;
            this.timestamp = timestamp;
        }
    }
}
