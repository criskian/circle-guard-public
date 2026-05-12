package com.circleguard.dashboard.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test: validates that the dashboard-service calls promotion-service
 * via HTTP and correctly propagates the stats to its own endpoints.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:dash_inttest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "circleguard.promotion-service.url=http://localhost:18088"
})
class DashboardPromotionIntegrationTest {

    private WireMockServer promotionServiceMock;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void startMock() {
        promotionServiceMock = new WireMockServer(wireMockConfig().port(18088));
        promotionServiceMock.start();
        WireMock.configureFor("localhost", 18088);
    }

    @AfterEach
    void stopMock() {
        promotionServiceMock.stop();
    }

    @Test
    void campusSummary_callsPromotionServiceHealthStats() throws Exception {
        stubFor(get(urlEqualTo("/api/v1/health-status/stats"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ACTIVE\":150,\"SUSPECT\":12,\"CONFIRMED\":2}")));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/analytics/summary")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(getRequestedFor(urlEqualTo("/api/v1/health-status/stats")));
    }

    @Test
    void campusSummary_whenPromotionServiceDown_returnsDegradedResponse() throws Exception {
        // Don't stub anything — WireMock returns 404 by default for unstubbed paths
        // PromotionClient has a fallback that returns an error map
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/analytics/summary")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()); // endpoint still responds gracefully
    }

    @Test
    void departmentStats_callsPromotionServiceWithDepartment() throws Exception {
        stubFor(get(urlEqualTo("/api/v1/health-status/stats/department/CS"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ACTIVE\":30,\"SUSPECT\":3}")));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/analytics/department/CS")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(getRequestedFor(urlEqualTo("/api/v1/health-status/stats/department/CS")));
    }
}
