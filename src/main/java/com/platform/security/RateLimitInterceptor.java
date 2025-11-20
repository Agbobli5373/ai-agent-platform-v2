package com.platform.security;

import com.platform.service.AuthorizationService;
import com.platform.service.RateLimitingService;
import io.quarkus.logging.Log;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

/**
 * Interceptor for rate limiting API requests
 */
@RateLimited
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class RateLimitInterceptor {

    @Inject
    RateLimitingService rateLimitingService;

    @Inject
    AuthorizationService authorizationService;

    @AroundInvoke
    public Object checkRateLimit(InvocationContext context) throws Exception {
        try {
            // Get current user ID
            UUID userId = authorizationService.getCurrentUserId();

            // Check rate limit
            if (!rateLimitingService.isAllowed(userId)) {
                int remaining = rateLimitingService.getRemainingRequests(userId, 100);
                Log.warnf("Rate limit exceeded for user %s", userId);

                throw new WebApplicationException(
                        Response.status(429) // Too Many Requests
                                .entity(new RateLimitError("Rate limit exceeded. Please try again later.", remaining))
                                .build()
                );
            }

            // Proceed with the method invocation
            return context.proceed();

        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            Log.errorf(e, "Error in rate limit interceptor");
            // Don't block request on rate limiting errors
            return context.proceed();
        }
    }

    public static class RateLimitError {
        public String error;
        public int remainingRequests;

        public RateLimitError(String error, int remainingRequests) {
            this.error = error;
            this.remainingRequests = remainingRequests;
        }
    }
}
