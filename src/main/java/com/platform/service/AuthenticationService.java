package com.platform.service;

import com.platform.domain.User;
import com.platform.exception.AuthenticationException;
import com.platform.repository.UserRepository;
import com.platform.security.JwtTokenProvider;
import com.platform.security.PasswordHasher;
import com.platform.security.dto.AuthenticationResponse;
import com.platform.security.dto.LoginRequest;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class AuthenticationService {

    private static final long SESSION_EXPIRATION_SECONDS = 86400L; // 24 hours

    @Inject
    UserRepository userRepository;

    @Inject
    PasswordHasher passwordHasher;

    @Inject
    JwtTokenProvider jwtTokenProvider;

    @Inject
    RedisDataSource redisDataSource;

    private ValueCommands<String, String> sessionCommands;

    @jakarta.annotation.PostConstruct
    void init() {
        sessionCommands = redisDataSource.value(String.class);
    }

    @Transactional
    public AuthenticationResponse authenticate(LoginRequest request) {
        if (request == null || request.email == null || request.email.isBlank()) {
            throw new AuthenticationException("Email is required");
        }
        if (request.password == null || request.password.isBlank()) {
            throw new AuthenticationException("Password is required");
        }

        User user = userRepository.findByEmail(request.email.trim().toLowerCase())
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));

        if (!passwordHasher.verifyPassword(request.password, user.passwordHash)) {
            throw new AuthenticationException("Invalid email or password");
        }

        user.lastLogin = LocalDateTime.now();
        userRepository.persist(user);

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        String sessionKey = "session:" + user.id.toString();
        sessionCommands.setex(sessionKey, SESSION_EXPIRATION_SECONDS, accessToken);

        AuthenticationResponse.UserInfo userInfo = new AuthenticationResponse.UserInfo(
                user.id,
                user.email,
                user.role,
                user.organization != null ? user.organization.id : null);

        return new AuthenticationResponse(
                accessToken,
                refreshToken,
                SESSION_EXPIRATION_SECONDS,
                userInfo);
    }

    public void invalidateSession(UUID userId) {
        String sessionKey = "session:" + userId.toString();
        sessionCommands.getdel(sessionKey);
    }

    public boolean isSessionValid(UUID userId) {
        String sessionKey = "session:" + userId.toString();
        return sessionCommands.get(sessionKey) != null;
    }

    @Transactional
    public AuthenticationResponse refreshToken(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new AuthenticationException("User ID is required");
        }

        UUID userUuid;
        try {
            userUuid = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new AuthenticationException("Invalid user ID format");
        }

        User user = userRepository.findByIdWithOrganization(userUuid)
                .orElseThrow(() -> new AuthenticationException("User not found"));

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        String sessionKey = "session:" + user.id.toString();
        sessionCommands.setex(sessionKey, SESSION_EXPIRATION_SECONDS, accessToken);

        AuthenticationResponse.UserInfo userInfo = new AuthenticationResponse.UserInfo(
                user.id,
                user.email,
                user.role,
                user.organization != null ? user.organization.id : null);

        return new AuthenticationResponse(
                accessToken,
                refreshToken,
                SESSION_EXPIRATION_SECONDS,
                userInfo);
    }
}
