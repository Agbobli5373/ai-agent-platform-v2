package com.platform.rest;

import com.platform.domain.ApiKey;
import com.platform.service.ApiKeyService;
import com.platform.service.AuthorizationService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST API endpoints for API key management
 */
@Path("/api/keys")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "API Keys", description = "Operations for managing API keys for programmatic access")
@SecurityRequirement(name = "jwt")
public class ApiKeyResource {

    @Inject
    ApiKeyService apiKeyService;

    @Inject
    AuthorizationService authorizationService;

    /**
     * Generate a new API key
     */
    @POST
    @Operation(summary = "Generate API key", description = "Creates a new API key for programmatic access")
    @APIResponses({
        @APIResponse(responseCode = "201", description = "API key created successfully",
                content = @Content(schema = @Schema(implementation = ApiKeyService.ApiKeyResponse.class))),
        @APIResponse(responseCode = "400", description = "Invalid request"),
        @APIResponse(responseCode = "401", description = "Unauthorized")
    })
    public Response generateApiKey(@Valid CreateApiKeyRequest request) {
        UUID userId = authorizationService.getCurrentUserId();

        ApiKeyService.ApiKeyResponse response = apiKeyService.generateApiKey(
                userId,
                request.name,
                request.description,
                request.expiresAt
        );

        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    /**
     * List API keys for the current user
     */
    @GET
    @Operation(summary = "List API keys", description = "Retrieves all API keys for the current user")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "API keys retrieved successfully"),
        @APIResponse(responseCode = "401", description = "Unauthorized")
    })
    public Response listApiKeys() {
        UUID userId = authorizationService.getCurrentUserId();

        List<ApiKey> apiKeys = apiKeyService.listApiKeys(userId);

        List<ApiKeyListResponse> responses = apiKeys.stream()
                .map(this::toApiKeyListResponse)
                .collect(Collectors.toList());

        return Response.ok(responses).build();
    }

    /**
     * Revoke an API key
     */
    @DELETE
    @Path("/{id}")
    @Operation(summary = "Revoke API key", description = "Revokes an API key, preventing further use")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "API key revoked successfully"),
        @APIResponse(responseCode = "404", description = "API key not found"),
        @APIResponse(responseCode = "401", description = "Unauthorized")
    })
    public Response revokeApiKey(@Parameter(description = "API key ID") @PathParam("id") UUID id) {
        UUID userId = authorizationService.getCurrentUserId();

        apiKeyService.revokeApiKey(id, userId);

        return Response.ok(new MessageResponse("API key revoked successfully")).build();
    }

    // Helper method
    private ApiKeyListResponse toApiKeyListResponse(ApiKey apiKey) {
        ApiKeyListResponse response = new ApiKeyListResponse();
        response.id = apiKey.id;
        response.name = apiKey.name;
        response.description = apiKey.description;
        response.createdAt = apiKey.createdAt;
        response.lastUsedAt = apiKey.lastUsedAt;
        response.expiresAt = apiKey.expiresAt;
        response.active = apiKey.active;
        response.expired = apiKey.isExpired();
        return response;
    }

    // Request/Response DTOs
    public static class CreateApiKeyRequest {
        public String name;
        public String description;
        public LocalDateTime expiresAt;
    }

    public static class ApiKeyListResponse {
        public UUID id;
        public String name;
        public String description;
        public LocalDateTime createdAt;
        public LocalDateTime lastUsedAt;
        public LocalDateTime expiresAt;
        public Boolean active;
        public Boolean expired;
    }

    public static class MessageResponse {
        public String message;

        public MessageResponse(String message) {
            this.message = message;
        }
    }
}
