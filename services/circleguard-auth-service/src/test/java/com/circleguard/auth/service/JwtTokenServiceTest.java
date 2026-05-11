package com.circleguard.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtTokenServiceTest {

    private static final String SECRET = "test-secret-key-minimum-256-bits-long-enough-for-hmac-sha256";
    private static final long EXPIRATION_MS = 3_600_000L; // 1 hour

    private JwtTokenService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtTokenService(SECRET, EXPIRATION_MS);
    }

    @Test
    void generateToken_returnsNonNullToken() {
        UUID anonymousId = UUID.randomUUID();
        Authentication auth = mockAuthWithPermissions("HEALTH_CENTER", "READ_STATS");

        String token = jwtService.generateToken(anonymousId, auth);

        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void generateToken_tokenContainsThreeParts() {
        UUID anonymousId = UUID.randomUUID();
        Authentication auth = mockAuthWithPermissions("ADMIN");

        String token = jwtService.generateToken(anonymousId, auth);

        // JWT format: header.payload.signature
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void generateToken_differentCallsProduceDifferentTokens() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Authentication auth = mockAuthWithPermissions("USER");

        String token1 = jwtService.generateToken(id1, auth);
        String token2 = jwtService.generateToken(id2, auth);

        // Two different subjects must yield different tokens
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void generateToken_expiredTokenGeneratedWithZeroTtl_isShort() {
        // A zero-expiration service should still produce a syntactically valid token
        JwtTokenService zeroTtlService = new JwtTokenService(SECRET, 0L);
        UUID anonymousId = UUID.randomUUID();
        Authentication auth = mockAuthWithPermissions("USER");

        String token = zeroTtlService.generateToken(anonymousId, auth);

        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void generateToken_withMultiplePermissions_tokenIsStillValid() {
        UUID anonymousId = UUID.randomUUID();
        Authentication auth = mockAuthWithPermissions(
                "HEALTH_CENTER", "ADMIN", "READ_STATS", "MANAGE_USERS", "alert:receive_priority");

        assertThatCode(() -> jwtService.generateToken(anonymousId, auth))
                .doesNotThrowAnyException();
    }

    // ── helpers ─────────────────────────────────────────────────────────────────

    private Authentication mockAuthWithPermissions(String... authorities) {
        Authentication auth = mock(Authentication.class);
        List<SimpleGrantedAuthority> grantedAuthorities = List.of(authorities)
                .stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        doReturn(grantedAuthorities).when(auth).getAuthorities();
        return auth;
    }
}
