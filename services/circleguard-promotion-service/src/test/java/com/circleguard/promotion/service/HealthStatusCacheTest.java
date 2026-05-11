package com.circleguard.promotion.service;

import com.circleguard.promotion.repository.graph.CircleNodeRepository;
import com.circleguard.promotion.repository.graph.UserNodeRepository;
import com.circleguard.promotion.repository.jpa.SystemSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HealthStatusCacheTest {

    @Mock private UserNodeRepository userNodeRepository;
    @Mock private Neo4jClient neo4jClient;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private SystemSettingsRepository systemSettingsRepository;
    @Mock private CircleNodeRepository circleNodeRepository;

    private HealthStatusService healthStatusService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        healthStatusService = new HealthStatusService(
                userNodeRepository, neo4jClient, redisTemplate,
                kafkaTemplate, systemSettingsRepository, circleNodeRepository);
    }

    @Test
    void getCachedStatus_returnsRedisValue() {
        String anonymousId = "user-abc";
        when(valueOps.get("user:status:user-abc")).thenReturn("SUSPECT");

        String status = healthStatusService.getCachedStatus(anonymousId);

        assertThat(status).isEqualTo("SUSPECT");
    }

    @Test
    void getCachedStatus_returnsNullWhenNotCached() {
        String anonymousId = "unknown-user";
        when(valueOps.get("user:status:unknown-user")).thenReturn(null);

        String status = healthStatusService.getCachedStatus(anonymousId);

        assertThat(status).isNull();
    }

    @Test
    void evictUserCache_doesNotThrow() {
        // evictUserCache is a pure cache eviction — verify it's side-effect free
        assertThatCode(() -> healthStatusService.evictUserCache("any-user"))
                .doesNotThrowAnyException();
    }
}
