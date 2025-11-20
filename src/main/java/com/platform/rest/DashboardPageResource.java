package com.platform.rest;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/dashboard")
public class DashboardPageResource {

    @Inject
    @io.quarkus.qute.Location("dashboard/home.html")
    Template dashboardHome;

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
}
