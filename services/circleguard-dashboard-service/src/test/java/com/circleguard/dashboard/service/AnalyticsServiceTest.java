package com.circleguard.dashboard.service;

import com.circleguard.dashboard.client.PromotionClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock private JdbcTemplate jdbc;
    @Mock private PromotionClient promotionClient;
    @Mock private KAnonymityFilter kAnonymityFilter;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(jdbc, promotionClient, kAnonymityFilter);
    }

    @Test
    void getCampusSummary_delegatesToPromotionClient() {
        Map<String, Object> fakeStats = Map.of("ACTIVE", 150, "SUSPECT", 5);
        when(promotionClient.getHealthStats()).thenReturn(fakeStats);

        Map<String, Object> result = analyticsService.getCampusSummary();

        assertThat(result).isEqualTo(fakeStats);
        verify(promotionClient).getHealthStats();
    }

    @Test
    void getDepartmentStats_appliesKAnonymityFilter() {
        String dept = "CS";
        Map<String, Object> raw = Map.of("ACTIVE", 3);
        Map<String, Object> filtered = Map.of("ACTIVE", "<5");

        when(promotionClient.getHealthStatsByDepartment(dept)).thenReturn(raw);
        when(kAnonymityFilter.apply(raw)).thenReturn(filtered);

        Map<String, Object> result = analyticsService.getDepartmentStats(dept);

        assertThat(result).isEqualTo(filtered);
    }

    @Test
    void getGlobalHealthStats_delegatesToCampusSummary() {
        Map<String, Object> stats = Map.of("total", 300);
        when(promotionClient.getHealthStats()).thenReturn(stats);

        Map<String, Object> result = analyticsService.getGlobalHealthStats();

        assertThat(result).containsKey("total");
    }

    @Test
    void getTimeSeries_whenDbFails_returnsMockData() {
        // JdbcTemplate throws when table doesn't exist — fallback returns generated data
        when(jdbc.queryForList(anyString(), (Object) any())).thenThrow(new RuntimeException("table not found"));

        List<Map<String, Object>> result = analyticsService.getTimeSeries("hourly", 10);

        // Mock generator returns up to 24 buckets × 4 statuses
        assertThat(result).isNotEmpty();
        assertThat(result.get(0)).containsKeys("bucket", "status", "total");
    }

    @Test
    void getEntryTrends_suppressesCountsBelowFive() {
        UUID locationId = UUID.randomUUID();
        List<Map<String, Object>> dbRows = List.of(
                Map.of("hour", "2026-01-01T10:00", "entry_count", 2L),
                Map.of("hour", "2026-01-01T11:00", "entry_count", 10L)
        );
        when(jdbc.queryForList(anyString(), eq(locationId))).thenReturn(
                // need mutable maps for the in-place mutation
                new java.util.ArrayList<>(List.of(
                        new java.util.HashMap<>(Map.of("hour", "2026-01-01T10:00", "entry_count", 2L)),
                        new java.util.HashMap<>(Map.of("hour", "2026-01-01T11:00", "entry_count", 10L))
                ))
        );

        List<Map<String, Object>> result = analyticsService.getEntryTrends(locationId);

        // Low-count row must be masked
        assertThat(result.get(0).get("entry_count")).isEqualTo("<5");
        // High-count row must pass through unchanged
        assertThat(result.get(1).get("entry_count")).isEqualTo(10L);
    }
}
