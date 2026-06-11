package com.circleguard.gateway.service;

import com.circleguard.gateway.config.FeatureFlags;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.security.Key;

@Service
@RequiredArgsConstructor
public class QrValidationService {

    private final StringRedisTemplate redisTemplate;
    private final FeatureFlags featureFlags;
    private final MeterRegistry meterRegistry;

    @Value("${qr.secret}")
    private String qrSecret;

    private static final String STATUS_KEY_PREFIX = "user:status:";

    public ValidationResult validateToken(String token) {
        try {
            Key key = Keys.hmacShaKeyFor(qrSecret.getBytes());
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String anonymousId = claims.getSubject();
            String status = fetchStatusFromRedis(anonymousId);

            if ("CONTAGIED".equals(status) || "POTENTIAL".equals(status)) {
                meterRegistry.counter("qr_validations_total", "result", "denied").increment();
                return new ValidationResult(false, "RED", "Access Denied: Health Risk Detected");
            }
            meterRegistry.counter("qr_validations_total", "result", "granted").increment();
            return new ValidationResult(true, "GREEN", "Welcome to Campus");

        } catch (Exception e) {
            meterRegistry.counter("qr_validations_total", "result", "invalid").increment();
            return new ValidationResult(false, "RED", "Invalid or Expired Token");
        }
    }

    @CircuitBreaker(name = "redis-status", fallbackMethod = "fetchStatusFallback")
    public String fetchStatusFromRedis(String anonymousId) {
        return redisTemplate.opsForValue().get(STATUS_KEY_PREFIX + anonymousId);
    }

    /**
     * Fallback when Redis is unavailable: deny access if strict validation is enabled,
     * allow access with a warning otherwise (configurable via feature flag).
     */
    public String fetchStatusFallback(String anonymousId, Throwable t) {
        if (featureFlags.isStrictQrValidation()) {
            return "POTENTIAL"; // deny on uncertainty when strict mode is on
        }
        return null; // treat as healthy when strict mode is off
    }

    public record ValidationResult(boolean valid, String status, String message) {}
}
