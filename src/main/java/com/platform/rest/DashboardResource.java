package com.platform.rest;

import com.platform.service.DashboardService;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/dashboard")
public class DashboardResource {

    @Inject
    @io.quarkus.qute.Location("dashboard/home.html")
    Template dashboardHome;

    @Inject
    @io.quarkus.qute.Location("wizard.html")
    Template wizardTemplate;

    @Inject
    DashboardService dashboardService;

    @GET
    @Produces(MediaType.TEXT_HTML)
    @jakarta.annotation.security.PermitAll
    public TemplateInstance home() {
        // Return dashboard template without user data
        // User data will be loaded via JavaScript using the JWT token
        return dashboardHome
                .data("userInitials", "U")
                .data("userName", "User")
                .data("userEmail", "user@example.com")
                .data("userRole", "USER")
                .data("currentOrgName", "My Organization");
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

    @GET
    @Path("/wizard")
    @Produces(MediaType.TEXT_HTML)
    @jakarta.annotation.security.PermitAll
    public TemplateInstance wizard() {
        // Return wizard template
        // User data will be loaded via JavaScript using the JWT token
        return wizardTemplate
                .data("userInitials", "U")
                .data("userName", "User")
                .data("userEmail", "user@example.com")
                .data("userRole", "USER")
                .data("currentOrgName", "My Organization");
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
