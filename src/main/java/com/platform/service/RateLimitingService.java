package com.platform.service;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;
import java.util.UUID;

/**
 * Rate limiting service using Redis for distributed rate limiting
 */
@ApplicationScoped
public class RateLimitingService {

    private static final int DEFAULT_RATE_LIMIT = 100; // requests per minute
    private static final Duration WINDOW_DURATION = Duration.ofMinutes(1);

    @Inject
    RedisDataSource redisDataSource;

    private ValueCommands<String, String> valueCommands;

    @jakarta.annotation.PostConstruct
    void init() {
        valueCommands = redisDataSource.value(String.class);
    }

    /**
     * Check if a request is allowed under rate limiting
     *
     * @param userId The user ID
     * @param limit  The rate limit (requests per minute)
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean isAllowed(UUID userId, int limit) {
        String key = "rate_limit:" + userId.toString();

        // Get current count
        String countStr = valueCommands.get(key);
        int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;

        if (currentCount >= limit) {
            return false;
        }

        // Increment counter
        if (countStr == null) {
            // First request in window - set with expiration
            valueCommands.setex(key, WINDOW_DURATION.getSeconds(), "1");
        } else {
            // Increment existing counter
            valueCommands.incr(key);
        }

        return true;
    }

    /**
     * Check if a request is allowed with default rate limit
     *
     * @param userId The user ID
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean isAllowed(UUID userId) {
        return isAllowed(userId, DEFAULT_RATE_LIMIT);
    }

    /**
     * Get remaining requests for a user
     *
     * @param userId The user ID
     * @param limit  The rate limit
     * @return Number of remaining requests
     */
    public int getRemainingRequests(UUID userId, int limit) {
        String key = "rate_limit:" + userId.toString();
        String countStr = valueCommands.get(key);
        int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;
        return Math.max(0, limit - currentCount);
    }

    /**
     * Reset rate limit for a user (for testing or admin purposes)
     *
     * @param userId The user ID
     */
    public void reset(UUID userId) {
        String key = "rate_limit:" + userId.toString();
        valueCommands.getdel(key);
    }
}
