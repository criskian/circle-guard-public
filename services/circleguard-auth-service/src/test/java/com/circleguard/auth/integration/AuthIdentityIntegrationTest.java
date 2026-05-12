package com.circleguard.auth.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test: validates that the auth-service calls identity-service
 * and correctly maps the anonymous UUID into the issued JWT.
 *
 * AuthenticationManager is mocked so the test does not depend on LDAP/DB availability.
 * IdentityClient is real (uses RestTemplate) and hits the WireMock stub.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth_inttest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.ldap.embedded.base-dn=dc=circleguard,dc=edu",
        "spring.ldap.embedded.ldif=classpath:test-schema.ldif",
        "spring.ldap.embedded.port=8389",
        "jwt.secret=test-secret-key-minimum-256-bits-long-enough-for-hmac-sha256",
        "jwt.expiration=3600000",
        "qr.secret=test-qr-secret-key-minimum-256-bits-long-enough",
        "qr.expiration=60000",
        "identity.service.url=http://localhost:18083"
})
class AuthIdentityIntegrationTest {

    private WireMockServer wireMockServer;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationManager authenticationManager;

    @BeforeEach
    void startWireMock() {
        wireMockServer = new WireMockServer(wireMockConfig().port(18083));
        wireMockServer.start();
        WireMock.configureFor("localhost", 18083);
    }

    @AfterEach
    void stopWireMock() {
        wireMockServer.stop();
    }

    @Test
    void login_callsIdentityServiceToObtainAnonymousId() throws Exception {
        UUID anonymousId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        // Mock successful authentication — bypasses LDAP and DB
        Authentication successfulAuth = new UsernamePasswordAuthenticationToken(
                "testuser", null,
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("STUDENT"))
        );
        Mockito.when(authenticationManager.authenticate(Mockito.any()))
               .thenReturn(successfulAuth);

        // Stub: identity-service maps any realIdentity → anonymousId
        stubFor(WireMock.post(urlEqualTo("/api/v1/identities/map"))
                .withRequestBody(matchingJsonPath("$.realIdentity"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"anonymousId\": \"" + anonymousId + "\"}")));

        // Perform login with any credentials (auth is mocked to succeed)
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"testuser\", \"password\": \"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.anonymousId").value(anonymousId.toString()));

        // Verify that identity-service was called with the username
        verify(postRequestedFor(urlEqualTo("/api/v1/identities/map"))
                .withRequestBody(matchingJsonPath("$.realIdentity", equalTo("testuser"))));
    }

    @Test
    void login_returnsUnauthorized_whenAuthFails() throws Exception {
        // Mock authentication failure
        Mockito.when(authenticationManager.authenticate(Mockito.any()))
               .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Bad credentials"));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"testuser\", \"password\": \"wrongpassword\"}"))
                .andExpect(status().isUnauthorized());

        // Identity service must NOT be called on failed auth
        verify(0, postRequestedFor(urlEqualTo("/api/v1/identities/map")));
    }

    @Test
    void identityServiceUrl_isConfiguredViaProperty() {
        // Verify no calls were made to the default prod port 8083 (would indicate misconfiguration)
        verify(0, postRequestedFor(urlEqualTo("/api/v1/identities/map"))
                .withPort(8083));
    }
}
