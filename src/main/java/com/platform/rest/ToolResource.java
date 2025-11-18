package com.platform.rest;

import com.platform.domain.Tool;
import com.platform.service.AuthorizationService;
import com.platform.service.ToolRegistryService;
import com.platform.service.dto.ToolExecutionResult;
import com.platform.service.dto.ToolRegistrationRequest;
import com.platform.service.dto.ToolResponse;
import com.platform.service.dto.ToolValidationResult;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Path("/api/tools")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ToolResource {

    private static final Logger LOG = Logger.getLogger(ToolResource.class);

    @Inject
    ToolRegistryService toolRegistryService;

    @Inject
    AuthorizationService authorizationService;

    @POST
    @RolesAllowed({"USER", "ADMIN"})
    public Response registerTool(ToolRegistrationRequest request) {
        LOG.infof("Registering new tool: %s", request.name);
        UUID userId = authorizationService.getCurrentUserId();
        UUID orgId = authorizationService.getCurrentOrganizationId();
        Tool tool = toolRegistryService.registerTool(request, userId, orgId);
        return Response.status(Response.Status.CREATED).entity(ToolResponse.from(tool)).build();
    }

    @GET
    @RolesAllowed({"USER", "ADMIN", "VIEWER"})
    public Response listTools() {
        LOG.info("Listing tools for organization");
        UUID orgId = authorizationService.getCurrentOrganizationId();
        List<Tool> tools = toolRegistryService.getToolsByOrganization(orgId);
        List<ToolResponse> response = tools.stream().map(ToolResponse::from).toList();
        return Response.ok(response).build();
    }

    @GET
    @Path("/{toolId}")
    @RolesAllowed({"USER", "ADMIN", "VIEWER"})
    public Response getTool(@PathParam("toolId") UUID toolId) {
        LOG.infof("Getting tool: %s", toolId);
        Optional<Tool> toolOpt = toolRegistryService.getToolById(toolId);
        if (toolOpt.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Tool not found"))
                    .build();
        }
        return Response.ok(ToolResponse.from(toolOpt.get())).build();
    }

    @PUT
    @Path("/{toolId}")
    @RolesAllowed({"USER", "ADMIN"})
    public Response updateTool(@PathParam("toolId") UUID toolId, ToolRegistrationRequest request) {
        LOG.infof("Updating tool: %s", toolId);
        UUID userId = authorizationService.getCurrentUserId();
        UUID orgId = authorizationService.getCurrentOrganizationId();
        Tool tool = toolRegistryService.updateTool(toolId, request, userId, orgId);
        return Response.ok(ToolResponse.from(tool)).build();
    }

    @DELETE
    @Path("/{toolId}")
    @RolesAllowed({"ADMIN"})
    public Response deleteTool(@PathParam("toolId") UUID toolId) {
        LOG.infof("Deleting tool: %s", toolId);
        UUID userId = authorizationService.getCurrentUserId();
        UUID orgId = authorizationService.getCurrentOrganizationId();
        toolRegistryService.deleteTool(toolId, userId, orgId);
        return Response.ok(Map.of("message", "Tool deleted successfully")).build();
    }

    @POST
    @Path("/{toolId}/validate")
    @RolesAllowed({"USER", "ADMIN"})
    public Response validateConnection(@PathParam("toolId") UUID toolId) {
        LOG.infof("Validating connection for tool: %s", toolId);
        ToolValidationResult result = toolRegistryService.validateConnection(toolId);
        if (result.valid) {
            return Response.ok(result).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity(result).build();
        }
    }

    @POST
    @Path("/{toolId}/execute")
    @RolesAllowed({"USER", "ADMIN"})
    public Response executeTool(@PathParam("toolId") UUID toolId, Map<String, Object> params) {
        LOG.infof("Executing tool: %s", toolId);
        ToolExecutionResult result = toolRegistryService.executeTool(toolId, params);
        if (result.success) {
            return Response.ok(result).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(result).build();
        }
    }

    @GET
    @Path("/type/{type}")
    @RolesAllowed({"USER", "ADMIN", "VIEWER"})
    public Response getToolsByType(@PathParam("type") Tool.ToolType type) {
        LOG.infof("Getting tools by type: %s", type);
        UUID orgId = authorizationService.getCurrentOrganizationId();
        List<Tool> tools = toolRegistryService.getToolsByOrganization(orgId)
                .stream()
                .filter(tool -> tool.type == type)
                .toList();
        return Response.ok(tools).build();
    }

    @GET
    @Path("/search")
    @RolesAllowed({"USER", "ADMIN", "VIEWER"})
    public Response searchTools(@QueryParam("q") String query) {
        LOG.infof("Searching tools with query: %s", query);
        UUID orgId = authorizationService.getCurrentOrganizationId();
        List<Tool> allTools = toolRegistryService.getToolsByOrganization(orgId);
        List<Tool> matchingTools = allTools.stream()
                .filter(tool -> tool.name.toLowerCase().contains(query.toLowerCase()) ||
                        (tool.description != null && tool.description.toLowerCase().contains(query.toLowerCase())))
                .toList();
        return Response.ok(matchingTools).build();
    }
}
