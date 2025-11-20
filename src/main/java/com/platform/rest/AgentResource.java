package com.platform.rest;

import com.platform.domain.Agent;
import com.platform.domain.AgentTool;
import com.platform.domain.Tool;
import com.platform.exception.ValidationException;
import com.platform.repository.AgentRepository;
import com.platform.service.AgentRuntimeService;
import com.platform.service.AgentWizardService;
import com.platform.service.AuthorizationService;
import com.platform.service.dto.AgentConfiguration;
import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST API endpoints for agent management
 */
@Path("/api/agents")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "Agent Management", description = "Operations for creating, managing, and interacting with AI agents")
@SecurityRequirement(name = "jwt")
public class AgentResource {

    @Inject
    AgentRepository agentRepository;

    @Inject
    AgentWizardService agentWizardService;

    @Inject
    AgentRuntimeService agentRuntimeService;

    @Inject
    AuthorizationService authorizationService;

    /**
     * Create a new agent
     */
    @POST
    @Transactional
    @Operation(summary = "Create a new agent", description = "Creates a new AI agent with the specified configuration")
    @APIResponses({
        @APIResponse(responseCode = "201", description = "Agent created successfully",
                content = @Content(schema = @Schema(implementation = AgentResponse.class))),
        @APIResponse(responseCode = "400", description = "Invalid request",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @APIResponse(responseCode = "401", description = "Unauthorized")
    })
    public Response createAgent(@Valid CreateAgentRequest request) {
        UUID userId = authorizationService.getCurrentUserId();

        // Convert request to AgentConfiguration
        AgentConfiguration config = new AgentConfiguration();
        config.name = request.name;
        config.description = request.description;
        config.systemPrompt = request.systemPrompt;
        config.modelName = request.modelName != null ? request.modelName : "mistral-large-latest";
        config.toolIds = request.toolIds;

        // Deploy the agent
        Agent agent = agentWizardService.deployAgent(config, userId);

        // Convert to response DTO
        AgentResponse response = toAgentResponse(agent);

        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    /**
     * List all agents for the current user's organization
     */
    @GET
    @Operation(summary = "List agents", description = "Retrieves all agents for the current user's organization")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Agents retrieved successfully"),
        @APIResponse(responseCode = "401", description = "Unauthorized")
    })
    public Response listAgents(
            @Parameter(description = "Filter by agent status") @QueryParam("status") String status,
            @Parameter(description = "Page number") @QueryParam("page") @DefaultValue("0") int page,
            @Parameter(description = "Page size") @QueryParam("size") @DefaultValue("20") int size) {

        UUID organizationId = authorizationService.getCurrentOrganizationId();

        // Build query
        String query = "organization.id = ?1";
        Object[] params = new Object[]{organizationId};

        if (status != null && !status.isBlank()) {
            try {
                Agent.AgentStatus agentStatus = Agent.AgentStatus.valueOf(status.toUpperCase());
                query += " and status = ?2";
                params = new Object[]{organizationId, agentStatus};
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Invalid status: " + status);
            }
        }

        // Query with pagination
        List<Agent> agents = agentRepository.find(query, params)
                .page(page, size)
                .list();

        // Convert to response DTOs
        List<AgentResponse> responses = agents.stream()
                .map(this::toAgentResponse)
                .collect(Collectors.toList());

        return Response.ok(responses).build();
    }

    /**
     * Get agent details by ID
     */
    @GET
    @Path("/{id}")
    @Operation(summary = "Get agent details", description = "Retrieves detailed information about a specific agent")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Agent found",
                content = @Content(schema = @Schema(implementation = AgentResponse.class))),
        @APIResponse(responseCode = "404", description = "Agent not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @APIResponse(responseCode = "401", description = "Unauthorized")
    })
    public Response getAgent(@Parameter(description = "Agent ID") @PathParam("id") UUID id) {
        Agent agent = agentRepository.findById(id);

        if (agent == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Agent not found"))
                    .build();
        }

        // Check organization access
        authorizationService.requireSameOrganization(agent.organization.id);

        AgentResponse response = toAgentResponse(agent);
        return Response.ok(response).build();
    }

    /**
     * Update an existing agent
     */
    @PUT
    @Path("/{id}")
    @Transactional
    public Response updateAgent(@PathParam("id") UUID id, @Valid UpdateAgentRequest request) {
        Agent agent = agentRepository.findById(id);

        if (agent == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Agent not found"))
                    .build();
        }

        // Check organization access
        authorizationService.requireSameOrganization(agent.organization.id);

        // Update fields
        if (request.name != null && !request.name.isBlank()) {
            agent.name = request.name.trim();
        }

        if (request.description != null) {
            agent.description = request.description.trim();
        }

        if (request.systemPrompt != null && !request.systemPrompt.isBlank()) {
            agent.systemPrompt = request.systemPrompt.trim();
        }

        if (request.status != null) {
            try {
                agent.status = Agent.AgentStatus.valueOf(request.status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Invalid status: " + request.status);
            }
        }

        if (request.modelName != null && !request.modelName.isBlank()) {
            agent.modelName = request.modelName;
        }

        // Update tools if provided
        if (request.toolIds != null) {
            agent.tools.clear();
            for (UUID toolId : request.toolIds) {
                Tool tool = Tool.findById(toolId);
                if (tool != null) {
                    AgentTool agentTool = new AgentTool(agent, tool);
                    agent.tools.add(agentTool);
                }
            }
        }

        agentRepository.persist(agent);

        AgentResponse response = toAgentResponse(agent);
        return Response.ok(response).build();
    }

    /**
     * Delete an agent
     */
    @DELETE
    @Path("/{id}")
    @Transactional
    public Response deleteAgent(@PathParam("id") UUID id) {
        Agent agent = agentRepository.findById(id);

        if (agent == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Agent not found"))
                    .build();
        }

        // Check organization access
        authorizationService.requireSameOrganization(agent.organization.id);

        // Soft delete by setting status to DELETED
        agent.status = Agent.AgentStatus.DELETED;
        agentRepository.persist(agent);

        return Response.ok(new MessageResponse("Agent deleted successfully")).build();
    }

    /**
     * Chat with an agent (synchronous)
     */
    @POST
    @Path("/{id}/chat")
    @Transactional
    @com.platform.security.RateLimited
    @Operation(summary = "Chat with agent", description = "Send a message to an agent and receive a synchronous response")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Message processed successfully",
                content = @Content(schema = @Schema(implementation = ChatResponse.class))),
        @APIResponse(responseCode = "404", description = "Agent not found"),
        @APIResponse(responseCode = "429", description = "Rate limit exceeded"),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response chat(
            @Parameter(description = "Agent ID") @PathParam("id") UUID id,
            @Valid ChatRequest request) {
        Agent agent = agentRepository.findById(id);

        if (agent == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Agent not found"))
                    .build();
        }

        // Check organization access
        authorizationService.requireSameOrganization(agent.organization.id);

        UUID userId = authorizationService.getCurrentUserId();

        // Process message
        try {
            AgentRuntimeService.AgentResponse response = agentRuntimeService.processMessage(
                    id,
                    request.message,
                    request.conversationId,
                    userId
            ).toCompletableFuture().get();

            ChatResponse chatResponse = new ChatResponse();
            chatResponse.conversationId = response.conversationId;
            chatResponse.message = response.content;
            chatResponse.messageId = response.messageId;
            chatResponse.timestamp = response.timestamp;
            chatResponse.citations = response.citations;

            return Response.ok(chatResponse).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to process message: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Stream chat with an agent
     */
    @POST
    @Path("/{id}/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @com.platform.security.RateLimited
    @Operation(summary = "Stream chat with agent", description = "Send a message to an agent and receive a streaming response")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Streaming response started"),
        @APIResponse(responseCode = "404", description = "Agent not found"),
        @APIResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public Multi<String> streamChat(
            @Parameter(description = "Agent ID") @PathParam("id") UUID id,
            @Valid ChatRequest request) {
        Agent agent = agentRepository.findById(id);

        if (agent == null) {
            return Multi.createFrom().failure(new NotFoundException("Agent not found"));
        }

        // Check organization access
        try {
            authorizationService.requireSameOrganization(agent.organization.id);
        } catch (Exception e) {
            return Multi.createFrom().failure(e);
        }

        UUID userId = authorizationService.getCurrentUserId();

        // Stream response
        return agentRuntimeService.streamResponse(
                id,
                request.message,
                request.conversationId,
                userId
        );
    }

    // Helper method to convert Agent to AgentResponse
    private AgentResponse toAgentResponse(Agent agent) {
        AgentResponse response = new AgentResponse();
        response.id = agent.id;
        response.name = agent.name;
        response.description = agent.description;
        response.systemPrompt = agent.systemPrompt;
        response.status = agent.status.name();
        response.modelName = agent.modelName;
        response.createdAt = agent.createdAt;
        response.updatedAt = agent.updatedAt;

        if (agent.owner != null) {
            response.ownerId = agent.owner.id;
        }

        if (agent.organization != null) {
            response.organizationId = agent.organization.id;
        }

        if (agent.tools != null && !agent.tools.isEmpty()) {
            response.toolIds = agent.tools.stream()
                    .map(at -> at.tool.id)
                    .collect(Collectors.toList());
        }

        return response;
    }

    // Request/Response DTOs
    public static class CreateAgentRequest {
        public String name;
        public String description;
        public String systemPrompt;
        public String modelName;
        public List<UUID> toolIds;
    }

    public static class UpdateAgentRequest {
        public String name;
        public String description;
        public String systemPrompt;
        public String status;
        public String modelName;
        public List<UUID> toolIds;
    }

    public static class AgentResponse {
        public UUID id;
        public String name;
        public String description;
        public String systemPrompt;
        public String status;
        public String modelName;
        public UUID ownerId;
        public UUID organizationId;
        public List<UUID> toolIds;
        public java.time.LocalDateTime createdAt;
        public java.time.LocalDateTime updatedAt;
    }

    public static class ChatRequest {
        public String message;
        public UUID conversationId;
    }

    public static class ChatResponse {
        public UUID conversationId;
        public String message;
        public UUID messageId;
        public java.time.LocalDateTime timestamp;
        public List<com.platform.service.dto.RAGContext.Citation> citations;
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
}
