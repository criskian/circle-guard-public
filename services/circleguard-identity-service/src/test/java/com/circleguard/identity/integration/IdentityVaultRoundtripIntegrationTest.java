package com.circleguard.identity.integration;

import com.circleguard.identity.model.IdentityMapping;
import com.circleguard.identity.repository.IdentityMappingRepository;
import com.circleguard.identity.service.IdentityVaultService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test: validates the full identity vault roundtrip —
 * encryption, storage, hash lookup, and resolution — using H2 in-memory DB
 * and embedded Kafka so no external infrastructure is required.
 */
@Tag("integration")
@SpringBootTest
@DirtiesContext
@EmbeddedKafka(
        partitions = 1,
        topics = {"audit.identity.accessed"},
        brokerProperties = {
                "offsets.topic.replication.factor=1",
                "transaction.state.log.replication.factor=1",
                "transaction.state.log.min.isr=1",
                "group.initial.rebalance.delay.ms=0",
                "min.insync.replicas=1"
        }
)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:identity_inttest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "vault.hash-salt=integration-test-salt",
        "vault.encryption-secret=AES-256-integration-secret-key!",
        "vault.encryption-salt=integration-test-enc-salt",
        "jwt.secret=int-test-jwt-secret-minimum-256-bits-long-enough",
        "jwt.expiration=3600000"
})
class IdentityVaultRoundtripIntegrationTest {

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

        UUID first  = vaultService.getOrCreateAnonymousId(realIdentity);
        UUID second = vaultService.getOrCreateAnonymousId(realIdentity);

        assertThat(first).isEqualTo(second);
        assertThat(repository.findByIdentityHash(
                repository.findById(first).map(IdentityMapping::getIdentityHash).orElseThrow()
        )).isPresent();
    }

    @Test
    void resolveRealIdentity_afterMapping_returnsOriginalIdentity() {
        String realIdentity = "resolve.test@university.edu";

        UUID anonymousId = vaultService.getOrCreateAnonymousId(realIdentity);
        String resolved  = vaultService.resolveRealIdentity(anonymousId);

        assertThat(resolved).isEqualTo(realIdentity);
    }

    @Test
    void twoDistinctIdentities_mapToTwoDifferentAnonymousIds() {
        UUID id1 = vaultService.getOrCreateAnonymousId("alice@university.edu");
        UUID id2 = vaultService.getOrCreateAnonymousId("bob@university.edu");

        assertThat(id1).isNotEqualTo(id2);
    }
}
