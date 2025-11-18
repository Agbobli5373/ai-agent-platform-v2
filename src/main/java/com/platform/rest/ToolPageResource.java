package com.platform.rest;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

@Path("/tools")
@Produces(MediaType.TEXT_HTML)
public class ToolPageResource {

    private static final Logger LOG = Logger.getLogger(ToolPageResource.class);

    @Inject
    @io.quarkus.qute.Location("tools/list.html")
    Template toolsList;

    @Inject
    @io.quarkus.qute.Location("tools/create.html")
    Template toolsCreate;

    @Inject
    @io.quarkus.qute.Location("tools/edit.html")
    Template toolsEdit;

    @GET
    @jakarta.annotation.security.PermitAll
    public TemplateInstance listTools() {
        LOG.info("Rendering tools list page");
        // Return template with placeholder data
        // Actual data will be loaded via JavaScript using the JWT token
        return toolsList
                .data("tools", List.of())
                .data("userInitials", "U")
                .data("userName", "User")
                .data("userEmail", "user@example.com")
                .data("userRole", "USER")
                .data("currentOrgName", "My Organization");
    }

    @GET
    @Path("/create")
    @jakarta.annotation.security.PermitAll
    public TemplateInstance createToolPage() {
        LOG.info("Rendering tool creation page");
        // Return template with placeholder data
        return toolsCreate
                .data("userInitials", "U")
                .data("userName", "User")
                .data("userEmail", "user@example.com")
                .data("userRole", "USER")
                .data("currentOrgName", "My Organization");
    }

    @GET
    @Path("/{toolId}/edit")
    @jakarta.annotation.security.PermitAll
    public TemplateInstance editToolPage(@PathParam("toolId") UUID toolId) {
        LOG.infof("Rendering tool edit page for: %s", toolId);
        // Return template with placeholder data
        // Actual tool data will be loaded via JavaScript
        return toolsEdit
                .data("toolId", toolId.toString())
                .data("userInitials", "U")
                .data("userName", "User")
                .data("userEmail", "user@example.com")
                .data("userRole", "USER")
                .data("currentOrgName", "My Organization");
    }
}
