package com.circleguard.form.integration;

import com.circleguard.form.model.HealthSurvey;
import com.circleguard.form.model.ValidationStatus;
import com.circleguard.form.service.HealthSurveyService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test: validates that form-service publishes survey.submitted
 * and certificate.validated events to Kafka after survey actions.
 */
@Tag("integration")
@SpringBootTest
@Import(FormKafkaIntegrationTest.KafkaTestListeners.class)
@EmbeddedKafka(
        partitions = 1,
        topics = {"survey.submitted", "certificate.validated"},
        brokerProperties = {
                "offsets.topic.replication.factor=1",
                "transaction.state.log.replication.factor=1",
                "transaction.state.log.min.isr=1",
                "group.initial.rebalance.delay.ms=0",
                "min.insync.replicas=1"
        }
)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:form_inttest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.listener.auto-startup=true"
})
@DirtiesContext
class FormKafkaIntegrationTest {

    @Autowired
    private HealthSurveyService surveyService;

    @Autowired
    private KafkaTestListeners testListeners;

    @Test
    void submitSurvey_publishesSurveySubmittedEvent() throws InterruptedException {
        HealthSurvey survey = new HealthSurvey();
        survey.setAnonymousId(UUID.randomUUID());
        survey.setHasFever(true);
        survey.setHasCough(false);

        surveyService.submitSurvey(survey);

        boolean received = testListeners.surveyLatch.await(10, TimeUnit.SECONDS);
        assertThat(received).as("survey.submitted event should be published to Kafka").isTrue();
    }

    @Test
    void validateSurvey_whenApproved_publishesCertificateValidatedEvent() throws InterruptedException {
        HealthSurvey survey = new HealthSurvey();
        UUID anonymousId = UUID.randomUUID();
        survey.setAnonymousId(anonymousId);
        survey.setAttachmentPath("/certs/test.pdf");
        HealthSurvey saved = surveyService.submitSurvey(survey);

        testListeners.certLatch = new CountDownLatch(1);

        surveyService.validateSurvey(saved.getId(), ValidationStatus.APPROVED, UUID.randomUUID());

        boolean received = testListeners.certLatch.await(10, TimeUnit.SECONDS);
        assertThat(received).as("certificate.validated event should be published when APPROVED").isTrue();
    }

    @TestConfiguration
    @Component
    static class KafkaTestListeners {
        private static final Logger log = LoggerFactory.getLogger(KafkaTestListeners.class);
        volatile CountDownLatch surveyLatch = new CountDownLatch(1);
        volatile CountDownLatch certLatch = new CountDownLatch(1);

        @KafkaListener(topics = "survey.submitted", groupId = "form-test-group-survey")
        void onSurveySubmitted(ConsumerRecord<String, Object> record) {
            log.info("[Kafka] Received survey.submitted event: key={} value={}", record.key(), record.value());
            surveyLatch.countDown();
        }

        @KafkaListener(topics = "certificate.validated", groupId = "form-test-group-cert")
        void onCertificateValidated(ConsumerRecord<String, Object> record) {
            log.info("[Kafka] Received certificate.validated event: key={} value={}", record.key(), record.value());
            certLatch.countDown();
        }
    }
}
