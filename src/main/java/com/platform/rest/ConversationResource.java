package com.platform.rest;

import com.platform.domain.Conversation;
import com.platform.domain.Message;
import com.platform.repository.ConversationRepository;
import com.platform.repository.MessageRepository;
import com.platform.service.AuthorizationService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST API endpoints for conversation management
 */
@Path("/api/conversations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "Conversations", description = "Operations for managing conversations and message history")
@SecurityRequirement(name = "jwt")
public class ConversationResource {

    @Inject
    ConversationRepository conversationRepository;

    @Inject
    MessageRepository messageRepository;

    @Inject
    AuthorizationService authorizationService;

    /**
     * Get conversation details by ID
     */
    @GET
    @Path("/{id}")
    @Operation(summary = "Get conversation", description = "Retrieves details of a specific conversation")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Conversation found",
                content = @Content(schema = @Schema(implementation = ConversationResponse.class))),
        @APIResponse(responseCode = "404", description = "Conversation not found"),
        @APIResponse(responseCode = "401", description = "Unauthorized")
    })
    public Response getConversation(@Parameter(description = "Conversation ID") @PathParam("id") UUID id) {
        Conversation conversation = conversationRepository.findById(id);

        if (conversation == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Conversation not found"))
                    .build();
        }

        // Check organization access
        if (conversation.agent != null && conversation.agent.organization != null) {
            authorizationService.requireSameOrganization(conversation.agent.organization.id);
        }

        ConversationResponse response = toConversationResponse(conversation);
        return Response.ok(response).build();
    }

    /**
     * Get messages for a conversation
     */
    @GET
    @Path("/{id}/messages")
    @Operation(summary = "Get conversation messages", description = "Retrieves message history for a conversation")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Messages retrieved successfully"),
        @APIResponse(responseCode = "404", description = "Conversation not found"),
        @APIResponse(responseCode = "401", description = "Unauthorized")
    })
    public Response getMessages(
            @Parameter(description = "Conversation ID") @PathParam("id") UUID id,
            @Parameter(description = "Page number") @QueryParam("page") @DefaultValue("0") int page,
            @Parameter(description = "Page size") @QueryParam("size") @DefaultValue("50") int size) {

        Conversation conversation = conversationRepository.findById(id);

        if (conversation == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Conversation not found"))
                    .build();
        }

        // Check organization access
        if (conversation.agent != null && conversation.agent.organization != null) {
            authorizationService.requireSameOrganization(conversation.agent.organization.id);
        }

        // Query messages with pagination
        List<Message> messages = messageRepository.find(
                "conversation.id = ?1 ORDER BY timestamp ASC",
                id
        ).page(page, size).list();

        // Convert to response DTOs
        List<MessageResponse> responses = messages.stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());

        return Response.ok(responses).build();
    }

    /**
     * List conversations for the current user
     */
    @GET
    public Response listConversations(
            @QueryParam("agentId") UUID agentId,
            @QueryParam("status") String status,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {

        UUID userId = authorizationService.getCurrentUserId();

        // Build query
        StringBuilder queryBuilder = new StringBuilder("user.id = ?1");
        List<Object> params = new java.util.ArrayList<>();
        params.add(userId);

        if (agentId != null) {
            queryBuilder.append(" and agent.id = ?").append(params.size() + 1);
            params.add(agentId);
        }

        if (status != null && !status.isBlank()) {
            try {
                Conversation.ConversationStatus convStatus = Conversation.ConversationStatus.valueOf(status.toUpperCase());
                queryBuilder.append(" and status = ?").append(params.size() + 1);
                params.add(convStatus);
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Invalid status: " + status))
                        .build();
            }
        }

        queryBuilder.append(" ORDER BY createdAt DESC");

        // Query with pagination
        List<Conversation> conversations = conversationRepository.find(
                queryBuilder.toString(),
                params.toArray()
        ).page(page, size).list();

        // Convert to response DTOs
        List<ConversationResponse> responses = conversations.stream()
                .map(this::toConversationResponse)
                .collect(Collectors.toList());

        return Response.ok(responses).build();
    }

    // Helper methods
    private ConversationResponse toConversationResponse(Conversation conversation) {
        ConversationResponse response = new ConversationResponse();
        response.id = conversation.id;
        response.startedAt = conversation.startedAt;
        response.endedAt = conversation.endedAt;
        response.createdAt = conversation.createdAt;
        response.status = conversation.status != null ? conversation.status.name() : null;
        response.satisfactionScore = conversation.satisfactionScore;

        if (conversation.agent != null) {
            response.agentId = conversation.agent.id;
            response.agentName = conversation.agent.name;
        }

        if (conversation.user != null) {
            response.userId = conversation.user.id;
        }

        // Count messages
        response.messageCount = conversation.messages != null ? conversation.messages.size() : 0;

        return response;
    }

    private MessageResponse toMessageResponse(Message message) {
        MessageResponse response = new MessageResponse();
        response.id = message.id;
        response.role = message.role != null ? message.role.name() : null;
        response.content = message.content;
        response.timestamp = message.timestamp;
        response.tokenCount = message.tokenCount;
        return response;
    }

    // Response DTOs
    public static class ConversationResponse {
        public UUID id;
        public UUID agentId;
        public String agentName;
        public UUID userId;
        public LocalDateTime startedAt;
        public LocalDateTime endedAt;
        public LocalDateTime createdAt;
        public String status;
        public Integer satisfactionScore;
        public int messageCount;
    }

    public static class MessageResponse {
        public UUID id;
        public String role;
        public String content;
        public LocalDateTime timestamp;
        public Integer tokenCount;
    }

    public static class ErrorResponse {
        public String error;

        public ErrorResponse(String error) {
            this.error = error;
        }
    }
}
