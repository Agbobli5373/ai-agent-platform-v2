package com.platform.security;

import com.platform.domain.User;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.Set;

@ApplicationScoped
public class JwtTokenProvider {

    @ConfigProperty(name = "mp.jwt.verify.issuer", defaultValue = "https://ai-agent-platform.com")
    String issuer;

    @ConfigProperty(name = "jwt.access-token.duration", defaultValue = "24h")
    String accessTokenDurationStr;

    @ConfigProperty(name = "jwt.refresh-token.duration", defaultValue = "168h")
    String refreshTokenDurationStr;

    private Duration getAccessTokenDuration() {
        return Duration.parse("PT" + accessTokenDurationStr.toUpperCase());
    }

    private Duration getRefreshTokenDuration() {
        return Duration.parse("PT" + refreshTokenDurationStr.toUpperCase());
    }

    public String generateAccessToken(User user) {
        return Jwt.issuer(issuer)
                .upn(user.email)
                .subject(user.id.toString())
                .groups(Set.of(user.role))
                .claim("organizationId", user.organization != null ? user.organization.id.toString() : null)
                .expiresIn(getAccessTokenDuration())
                .sign();
    }

    public String generateRefreshToken(User user) {
        return Jwt.issuer(issuer)
                .upn(user.email)
                .subject(user.id.toString())
                .claim("type", "refresh")
                .expiresIn(getRefreshTokenDuration())
                .sign();
    }
}
