package com.platform.rest;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Resource for serving wizard UI pages
 */
@Path("/wizard")
public class WizardPageResource {

    @Inject
    Template wizard;

    @GET
    @Produces(MediaType.TEXT_HTML)
    @PermitAll
    public TemplateInstance getWizard() {
        // Return wizard template without user data
        // User data will be loaded via JavaScript using the JWT token
        return wizard
                .data("userInitials", "U")
                .data("userName", "User")
                .data("userEmail", "user@example.com")
                .data("userRole", "USER")
                .data("currentOrgName", "My Organization");
    }
}
