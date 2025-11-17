package com.platform.rest;

import com.platform.domain.Agent;
import com.platform.service.AgentWizardService;
import com.platform.service.AuthorizationService;
import com.platform.service.dto.*;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

/**
 * REST endpoints for the agent creation wizard
 */
@Path("/api/wizard")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class WizardResource {

    @Inject
    AgentWizardService wizardService;

    @Inject
    AuthorizationService authorizationService;

    /**
     * Initialize a new wizard session
     */
    @POST
    @Path("/session")
    public Response initializeSession() {
        UUID userId = authorizationService.getCurrentUserId();
        AgentWizardSession session = wizardService.createSession(userId);

        return Response.status(Response.Status.CREATED)
                .entity(new SessionResponse(session.sessionId, session.currentStep))
                .build();
    }

    /**
     * Get current wizard session
     */
    @GET
    @Path("/session/{sessionId}")
    public Response getSession(@PathParam("sessionId") UUID sessionId) {
        AgentWizardSession session = wizardService.getSession(sessionId);

        if (session == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Session not found or expired"))
                    .build();
        }

        // Verify the session belongs to the current user
        UUID currentUserId = authorizationService.getCurrentUserId();
        if (!session.userId.equals(currentUserId)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(new ErrorResponse("Access denied"))
                    .build();
        }

        return Response.ok(session).build();
    }

    /**
     * Save wizard step data
     */
    @PUT
    @Path("/session/{sessionId}/step")
    public Response saveStep(
            @PathParam("sessionId") UUID sessionId,
            @Valid SaveStepRequest request) {

        // Verify session exists and belongs to current user
        AgentWizardSession session = wizardService.getSession(sessionId);
        if (session == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Session not found or expired"))
                    .build();
        }

        UUID currentUserId = authorizationService.getCurrentUserId();
        if (!session.userId.equals(currentUserId)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(new ErrorResponse("Access denied"))
                    .build();
        }

        wizardService.saveStep(sessionId, request.step, request.data);

        return Response.ok(new MessageResponse("Step saved successfully")).build();
    }

    /**
     * Validate agent configuration
     */
    @POST
    @Path("/validate")
    public Response validateConfiguration(@Valid AgentConfiguration config) {
        ValidationResult result = wizardService.validateConfiguration(config);

        if (result.valid) {
            return Response.ok(result).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(result)
                    .build();
        }
    }

    /**
     * Preview agent with test prompt
     */
    @POST
    @Path("/preview")
    public Response previewAgent(@Valid PreviewRequest request) {
        ValidationResult validation = wizardService.validateConfiguration(request.config);

        if (!validation.valid) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid configuration: " +
                            String.join(", ", validation.errors)))
                    .build();
        }

        PreviewResponse preview = wizardService.previewAgent(request.config, request.testPrompt);

        return Response.ok(preview).build();
    }

    /**
     * Deploy agent
     */
    @POST
    @Path("/deploy")
    public Response deployAgent(@Valid DeployRequest request) {
        UUID userId = authorizationService.getCurrentUserId();

        // If sessionId is provided, verify and delete the session
        if (request.sessionId != null) {
            AgentWizardSession session = wizardService.getSession(request.sessionId);
            if (session != null && !session.userId.equals(userId)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(new ErrorResponse("Access denied"))
                        .build();
            }
        }

        Agent agent = wizardService.deployAgent(request.config, userId);

        // Clean up wizard session if provided
        if (request.sessionId != null) {
            wizardService.deleteSession(request.sessionId);
        }

        return Response.status(Response.Status.CREATED)
                .entity(new DeployResponse(agent.id, agent.name, agent.status.name()))
                .build();
    }

    /**
     * Delete wizard session
     */
    @DELETE
    @Path("/session/{sessionId}")
    public Response deleteSession(@PathParam("sessionId") UUID sessionId) {
        // Verify session belongs to current user
        AgentWizardSession session = wizardService.getSession(sessionId);
        if (session != null) {
            UUID currentUserId = authorizationService.getCurrentUserId();
            if (!session.userId.equals(currentUserId)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(new ErrorResponse("Access denied"))
                        .build();
            }
        }

        wizardService.deleteSession(sessionId);
        return Response.ok(new MessageResponse("Session deleted successfully")).build();
    }

    // Response DTOs
    public static class SessionResponse {
        public UUID sessionId;
        public AgentWizardSession.WizardStep currentStep;

        public SessionResponse(UUID sessionId, AgentWizardSession.WizardStep currentStep) {
            this.sessionId = sessionId;
            this.currentStep = currentStep;
        }
    }

    public static class MessageResponse {
        public String message;

        public MessageResponse(String message) {
            this.message = message;
        }
    }

    public static class ErrorResponse {
        public String error;

        public ErrorResponse(String error) {
            this.error = error;
        }
    }

    public static class DeployResponse {
        public UUID agentId;
        public String name;
        public String status;

        public DeployResponse(UUID agentId, String name, String status) {
            this.agentId = agentId;
            this.name = name;
            this.status = status;
        }
    }
}
