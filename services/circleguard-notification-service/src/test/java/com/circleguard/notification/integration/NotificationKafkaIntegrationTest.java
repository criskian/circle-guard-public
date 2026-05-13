package com.circleguard.notification.integration;

import com.circleguard.notification.service.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration test: validates that ExposureNotificationListener processes
 * promotion.status.changed Kafka events end-to-end using an embedded broker.
 */
@Tag("integration")
@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = {"promotion.status.changed"}
)
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.listener.auto-startup=true",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "management.health.mail.enabled=false"
})
@DirtiesContext
class NotificationKafkaIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockBean
    private NotificationDispatcher dispatcher;

    @MockBean
    private LmsService lmsService;

    @MockBean
    private AuditLogService auditLogService;

    @MockBean
    private EmailService emailService;

    @MockBean
    private SmsService smsService;

    @MockBean
    private PushService pushService;

    @MockBean
    private RoomReservationService roomReservationService;

    @MockBean
    private TemplateService templateService;

    @MockBean
    private org.springframework.mail.javamail.JavaMailSender mailSender;

    @MockBean
    private org.springframework.web.reactive.function.client.WebClient.Builder webClientBuilder;

    @Test
    void exposureListener_onExposedStatus_dispatchesNotificationAndSyncsLms() throws Exception {
        String eventJson = "{\"anonymousId\":\"user-abc\",\"status\":\"EXPOSED\"}";

        kafkaTemplate.send("promotion.status.changed", eventJson).get();

        Mockito.verify(dispatcher, Mockito.timeout(10_000)).dispatch("user-abc", "EXPOSED");
        Mockito.verify(lmsService, Mockito.timeout(10_000)).syncRemoteAttendance("user-abc", "EXPOSED");
    }

    @Test
    void exposureListener_onActiveStatus_doesNotDispatch() throws Exception {
        String eventJson = "{\"anonymousId\":\"user-xyz\",\"status\":\"ACTIVE\"}";

        kafkaTemplate.send("promotion.status.changed", eventJson).get();

        Thread.sleep(3_000);
        Mockito.verify(dispatcher, Mockito.never()).dispatch(Mockito.anyString(), Mockito.eq("ACTIVE"));
    }
}
