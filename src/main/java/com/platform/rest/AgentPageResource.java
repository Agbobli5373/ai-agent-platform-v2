package com.platform.rest;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.UUID;

/**
 * Page resources for agent management UI
 */
@Path("/agents")
public class AgentPageResource {

    @Inject
    @io.quarkus.qute.Location("agents/list.html")
    Template agentsList;

    @Inject
    @io.quarkus.qute.Location("agents/detail.html")
    Template agentsDetail;

    @Inject
    @io.quarkus.qute.Location("agents/chat.html")
    Template agentsChat;

    /**
     * Agent list page
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    @jakarta.annotation.security.PermitAll
    public TemplateInstance list() {
        // Return template with placeholder data
        // Actual data will be loaded via JavaScript using the JWT token
        return agentsList
                .data("userInitials", "U")
                .data("userName", "User")
                .data("userEmail", "user@example.com")
                .data("userRole", "USER")
                .data("currentOrgName", "My Organization");
    }

    /**
     * Agent detail page
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.TEXT_HTML)
    @jakarta.annotation.security.PermitAll
    public TemplateInstance detail(@PathParam("id") UUID id) {
        // Return template with placeholder data
        // Actual data will be loaded via JavaScript using the JWT token
        return agentsDetail
                .data("userInitials", "U")
                .data("userName", "User")
                .data("userEmail", "user@example.com")
                .data("userRole", "USER")
                .data("currentOrgName", "My Organization")
                .data("agentId", id.toString());
    }

    /**
     * Agent chat page
     */
    @GET
    @Path("/{id}/chat")
    @Produces(MediaType.TEXT_HTML)
    @jakarta.annotation.security.PermitAll
    public TemplateInstance chat(@PathParam("id") UUID id) {
        // Return template with placeholder data
        // Actual data will be loaded via JavaScript using the JWT token
        return agentsChat
                .data("userInitials", "U")
                .data("userName", "User")
                .data("userEmail", "user@example.com")
                .data("userRole", "USER")
                .data("currentOrgName", "My Organization")
                .data("agentId", id.toString());
    }
}
