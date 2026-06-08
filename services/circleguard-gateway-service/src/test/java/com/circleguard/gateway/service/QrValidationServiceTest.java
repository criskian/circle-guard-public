package com.circleguard.gateway.service;

import com.circleguard.gateway.config.FeatureFlags;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class QrValidationServiceTest {

    private QrValidationService service;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;
    private FeatureFlags featureFlags;
    private final String secret = "my-super-secret-test-key-32-chars-long";

    @BeforeEach
    void setUp() {
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        valueOps = Mockito.mock(ValueOperations.class);
        Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOps);

        featureFlags = new FeatureFlags();
        featureFlags.setStrictQrValidation(true);

        service = new QrValidationService(redisTemplate, featureFlags);
        ReflectionTestUtils.setField(service, "qrSecret", secret);
    }

    @Test
    void shouldAllowAccessForHealthyUser() {
        String anonymousId = UUID.randomUUID().toString();
        Key key = Keys.hmacShaKeyFor(secret.getBytes());
        String token = Jwts.builder()
                .setSubject(anonymousId)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        Mockito.when(valueOps.get("user:status:" + anonymousId)).thenReturn("CLEAR");

        QrValidationService.ValidationResult result = service.validateToken(token);

        assertTrue(result.valid());
        assertEquals("GREEN", result.status());
    }

    @Test
    void shouldDenyAccessForContagiedUser() {
        String anonymousId = UUID.randomUUID().toString();
        Key key = Keys.hmacShaKeyFor(secret.getBytes());
        String token = Jwts.builder()
                .setSubject(anonymousId)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        Mockito.when(valueOps.get("user:status:" + anonymousId)).thenReturn("CONTAGIED");

        QrValidationService.ValidationResult result = service.validateToken(token);

        assertFalse(result.valid());
        assertEquals("RED", result.status());
    }

    @Test
    void fallback_strictMode_deniesAccessWhenRedisUnavailable() {
        // When Redis is down and strict mode is ON → deny access (deny-on-uncertainty)
        featureFlags.setStrictQrValidation(true);
        RuntimeException redisDown = new RuntimeException("Redis connection refused");

        String result = service.fetchStatusFallback("any-user-id", redisDown);

        assertEquals("POTENTIAL", result);
    }

    @Test
    void fallback_nonStrictMode_allowsAccessWhenRedisUnavailable() {
        // When Redis is down and strict mode is OFF → allow access
        featureFlags.setStrictQrValidation(false);
        RuntimeException redisDown = new RuntimeException("Redis connection refused");

        String result = service.fetchStatusFallback("any-user-id", redisDown);

        assertNull(result);
    }

    @Test
    void shouldDenyAccessForInvalidToken() {
        QrValidationService.ValidationResult result = service.validateToken("invalid.token.here");

        assertFalse(result.valid());
        assertEquals("RED", result.status());
    }
}
