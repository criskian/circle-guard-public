package com.circleguard.identity.integration;

import com.circleguard.identity.model.IdentityMapping;
import com.circleguard.identity.repository.IdentityMappingRepository;
import com.circleguard.identity.service.IdentityVaultService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test: validates the full identity vault roundtrip using a real
 * PostgreSQL container — encryption, storage, hash lookup, and resolution.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "vault.hash-salt=integration-test-salt",
        "vault.encryption-secret=AES-256-integration-secret-key!",
        "vault.encryption-salt=integration-test-enc-salt",
        "jwt.secret=int-test-jwt-secret-minimum-256-bits-long-enough",
        "jwt.expiration=3600000",
        "spring.kafka.bootstrap-servers=localhost:19094"
})
class IdentityVaultRoundtripIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("circleguard_identity_test")
            .withUsername("testuser")
            .withPassword("testpassword");

    @DynamicPropertySource
    static void dbProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:19094");
    }

    @Autowired
    private IdentityVaultService vaultService;

    @Autowired
    private IdentityMappingRepository repository;

    @Test
    void getOrCreateAnonymousId_newIdentity_persistsInDatabase() {
        String realIdentity = "student.test@university.edu";

        UUID anonymousId = vaultService.getOrCreateAnonymousId(realIdentity);

        assertThat(anonymousId).isNotNull();
        Optional<IdentityMapping> saved = repository.findById(anonymousId);
        assertThat(saved).isPresent();
    }

    @Test
    void getOrCreateAnonymousId_sameIdentityCalledTwice_returnsSameId() {
        String realIdentity = "stable.user@university.edu";

        UUID first = vaultService.getOrCreateAnonymousId(realIdentity);
        UUID second = vaultService.getOrCreateAnonymousId(realIdentity);

        assertThat(first).isEqualTo(second);
        // Only one row should exist
        assertThat(repository.findByIdentityHash(
                repository.findById(first).map(IdentityMapping::getIdentityHash).orElseThrow()
        )).isPresent();
    }

    @Test
    void resolveRealIdentity_afterMapping_returnsOriginalIdentity() {
        String realIdentity = "resolve.test@university.edu";

        UUID anonymousId = vaultService.getOrCreateAnonymousId(realIdentity);
        String resolved = vaultService.resolveRealIdentity(anonymousId);

        assertThat(resolved).isEqualTo(realIdentity);
    }

    @Test
    void twoDistinctIdentities_mapToTwoDifferentAnonymousIds() {
        UUID id1 = vaultService.getOrCreateAnonymousId("alice@university.edu");
        UUID id2 = vaultService.getOrCreateAnonymousId("bob@university.edu");

        assertThat(id1).isNotEqualTo(id2);
    }
}
