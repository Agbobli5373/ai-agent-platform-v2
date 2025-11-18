package com.platform.rest;

import com.platform.domain.User;
import com.platform.security.dto.AuthenticationResponse;
import com.platform.security.dto.LoginRequest;
import com.platform.security.dto.RefreshTokenRequest;
import com.platform.security.dto.RegistrationRequest;
import com.platform.service.AuthenticationService;
import com.platform.service.AuthorizationService;
import com.platform.service.RegistrationService;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.UUID;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthenticationService authenticationService;

    @Inject
    RegistrationService registrationService;

    @Inject
    AuthorizationService authorizationService;

    @Inject
    JsonWebToken jwt;

    @Inject
    com.platform.repository.UserRepository userRepository;

    /**
     * Login endpoint
     */
    @POST
    @Path("/login")
    @PermitAll
    public Response login(@Valid LoginRequest request) {
        AuthenticationResponse response = authenticationService.authenticate(request);
        return Response.ok(response).build();
    }

    /**
     * Logout endpoint
     */
    @POST
    @Path("/logout")
    @Authenticated
    public Response logout() {
        UUID userId = authorizationService.getCurrentUserId();
        authenticationService.invalidateSession(userId);
        return Response.ok().entity(new MessageResponse("Logged out successfully")).build();
    }

    /**
     * Token refresh endpoint
     */
    @POST
    @Path("/refresh")
    @Authenticated
    public Response refresh(@Valid RefreshTokenRequest request) {
        // Verify the refresh token claim
        String tokenType = jwt.getClaim("type");
        if (!"refresh".equals(tokenType)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid token type"))
                    .build();
        }

        String userId = jwt.getSubject();
        AuthenticationResponse response = authenticationService.refreshToken(userId);
        return Response.ok(response).build();
    }

    /**
     * User registration endpoint
     */
    @POST
    @Path("/register")
    @PermitAll
    public Response register(@Valid RegistrationRequest request) {
        User user = registrationService.registerUser(request);

        // Automatically log in the user after registration
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.email = request.email;
        loginRequest.password = request.password;

        AuthenticationResponse response = authenticationService.authenticate(loginRequest);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    /**
     * Get current user info
     */
    @GET
    @Path("/me")
    @Authenticated
    public Response getCurrentUser() {
        UUID userId = authorizationService.getCurrentUserId();
        User user = userRepository.findByIdWithOrganization(userId)
                .orElseThrow(() -> new jakarta.ws.rs.NotFoundException("User not found"));

        String organizationName = user.organization != null ? user.organization.name : "No Organization";
        
        UserInfoResponse userInfo = new UserInfoResponse(
                user.id,
                user.email,
                user.role,
                user.organization != null ? user.organization.id : null,
                organizationName);

        return Response.ok(userInfo).build();
    }

    public static class UserInfoResponse {
        public UUID id;
        public String email;
        public String role;
        public UUID organizationId;
        public String organizationName;

        public UserInfoResponse(UUID id, String email, String role, UUID organizationId, String organizationName) {
            this.id = id;
            this.email = email;
            this.role = role;
            this.organizationId = organizationId;
            this.organizationName = organizationName;
        }
    }

    // Helper classes for responses
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
