package com.platform.service;

import com.platform.domain.ApiKey;
import com.platform.domain.User;
import com.platform.exception.ValidationException;
import com.platform.repository.ApiKeyRepository;
import com.platform.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.mindrot.jbcrypt.BCrypt;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing API keys
 */
@ApplicationScoped
public class ApiKeyService {

    private static final int API_KEY_LENGTH = 32; // 32 bytes = 256 bits
    private static final String API_KEY_PREFIX = "aap_"; // AI Agent Platform prefix

    @Inject
    ApiKeyRepository apiKeyRepository;

    @Inject
    UserRepository userRepository;

    /**
     * Generate a new API key for a user
     *
     * @param userId      The user ID
     * @param name        The API key name
     * @param description Optional description
     * @param expiresAt   Optional expiration date
     * @return Generated API key (plain text - only shown once)
     */
    @Transactional
    public ApiKeyResponse generateApiKey(UUID userId, String name, String description, LocalDateTime expiresAt) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("API key name is required");
        }

        User user = userRepository.findByIdWithOrganization(userId)
                .orElseThrow(() -> new ValidationException("User not found"));

        // Generate random API key
        String plainKey = generateRandomKey();
        String fullKey = API_KEY_PREFIX + plainKey;

        // Hash the key for storage
        String keyHash = BCrypt.hashpw(fullKey, BCrypt.gensalt());

        // Create API key entity
        ApiKey apiKey = new ApiKey();
        apiKey.keyHash = keyHash;
        apiKey.name = name.trim();
        apiKey.description = description != null ? description.trim() : null;
        apiKey.user = user;
        apiKey.organization = user.organization;
        apiKey.expiresAt = expiresAt;
        apiKey.active = true;

        apiKeyRepository.persist(apiKey);

        // Return the plain key (only time it's visible)
        return new ApiKeyResponse(apiKey.id, fullKey, apiKey.name, apiKey.createdAt, apiKey.expiresAt);
    }

    /**
     * Validate an API key
     *
     * @param apiKey The API key to validate
     * @return The ApiKey entity if valid, null otherwise
     */
    @Transactional
    public ApiKey validateApiKey(String apiKey) {
        if (apiKey == null || !apiKey.startsWith(API_KEY_PREFIX)) {
            return null;
        }

        // Find all active API keys and check against each hash
        List<ApiKey> activeKeys = apiKeyRepository.find("active = true").list();

        for (ApiKey key : activeKeys) {
            if (BCrypt.checkpw(apiKey, key.keyHash)) {
                if (key.isValid()) {
                    // Update last used timestamp
                    key.lastUsedAt = LocalDateTime.now();
                    apiKeyRepository.persist(key);
                    return key;
                }
                return null; // Key found but expired or inactive
            }
        }

        return null; // No matching key found
    }

    /**
     * List API keys for a user
     *
     * @param userId The user ID
     * @return List of API keys (without the actual key values)
     */
    public List<ApiKey> listApiKeys(UUID userId) {
        return apiKeyRepository.find("user.id = ?1 ORDER BY createdAt DESC", userId).list();
    }

    /**
     * Revoke an API key
     *
     * @param apiKeyId The API key ID
     * @param userId   The user ID (for authorization)
     */
    @Transactional
    public void revokeApiKey(UUID apiKeyId, UUID userId) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId);

        if (apiKey == null) {
            throw new ValidationException("API key not found");
        }

        if (!apiKey.user.id.equals(userId)) {
            throw new ValidationException("Unauthorized to revoke this API key");
        }

        apiKey.active = false;
        apiKeyRepository.persist(apiKey);
    }

    /**
     * Generate a random API key
     */
    private String generateRandomKey() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[API_KEY_LENGTH];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Response DTO for API key generation
     */
    public static class ApiKeyResponse {
        public UUID id;
        public String key; // Plain text key - only shown once
        public String name;
        public LocalDateTime createdAt;
        public LocalDateTime expiresAt;

        public ApiKeyResponse(UUID id, String key, String name, LocalDateTime createdAt, LocalDateTime expiresAt) {
            this.id = id;
            this.key = key;
            this.name = name;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
        }
    }
}
