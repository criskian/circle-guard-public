package com.circleguard.promotion.integration;

import com.circleguard.promotion.service.HealthStatusService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

/**
 * Integration test: validates that survey.submitted Kafka events trigger
 * HealthStatusService.updateStatus(SUSPECT) inside promotion-service.
 */
@Tag("integration")
@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@ActiveProfiles("test")
@DirtiesContext
@EmbeddedKafka(
        partitions = 1,
        topics = {"survey.submitted", "certificate.validated", "promotion.status.changed"}
)
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
        "spring.kafka.producer.properties.spring.json.add.type.headers=false",
        "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer",
        "spring.kafka.consumer.properties.spring.json.trusted.packages=*",
        "spring.kafka.consumer.properties.spring.json.use.type.headers=false",
        "spring.kafka.consumer.properties.spring.json.value.default.type=java.util.LinkedHashMap",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.neo4j.uri=bolt://localhost:17687",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=16379",
        "spring.datasource.url=jdbc:h2:mem:promo_inttest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false"
})
class SurveyToStatusIntegrationTest {

    @TestConfiguration
    static class TestConfig {
        @Bean @Primary
        PlatformTransactionManager transactionManager() {
            return Mockito.mock(PlatformTransactionManager.class);
        }

        @Bean(name = "neo4jTransactionManager")
        PlatformTransactionManager neo4jTransactionManager() {
            return Mockito.mock(PlatformTransactionManager.class);
        }
    }

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockBean
    private HealthStatusService healthStatusService;

    @MockBean
    private org.springframework.data.neo4j.core.Neo4jClient neo4jClient;

    @MockBean
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @MockBean
    private com.circleguard.promotion.repository.graph.UserNodeRepository userNodeRepository;

    @MockBean
    private com.circleguard.promotion.repository.jpa.SystemSettingsRepository systemSettingsRepository;

    @MockBean
    private com.circleguard.promotion.repository.graph.CircleNodeRepository circleNodeRepository;

    @MockBean
    private org.springframework.cache.CacheManager cacheManager;

    @Test
    void surveySubmittedEvent_withSymptoms_triggersUpdateStatusSuspect() {
        Map<String, Object> event = Map.of(
                "anonymousId", "integration-user-001",
                "hasSymptoms", true,
                "timestamp", System.currentTimeMillis()
        );

        kafkaTemplate.send("survey.submitted", "integration-user-001", event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                verify(healthStatusService, atLeastOnce()).updateStatus("integration-user-001", "SUSPECT")
        );
    }

    @Test
    void certificateValidatedEvent_withApproved_triggersUpdateStatusActive() {
        Map<String, Object> event = Map.of(
                "anonymousId", "integration-user-002",
                "status", "APPROVED",
                "timestamp", System.currentTimeMillis()
        );

        kafkaTemplate.send("certificate.validated", "integration-user-002", event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                verify(healthStatusService, atLeastOnce()).updateStatus("integration-user-002", "ACTIVE")
        );
    }
}
