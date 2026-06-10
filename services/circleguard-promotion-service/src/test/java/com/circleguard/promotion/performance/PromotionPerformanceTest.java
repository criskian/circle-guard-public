package com.circleguard.promotion.performance;

import com.circleguard.promotion.service.HealthStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:promo_perf_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
public class PromotionPerformanceTest {

    @Container
    static Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:5.12")
            .withAdminPassword("password");

    @DynamicPropertySource
    static void neo4jProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.neo4j.uri", neo4jContainer::getBoltUrl);
        registry.add("spring.neo4j.authentication.username", () -> "neo4j");
        registry.add("spring.neo4j.authentication.password", () -> "password");
    }

    @Autowired
    private HealthStatusService healthStatusService;
    
    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @Autowired
    private Neo4jClient neo4jClient;

    private String rootUser;

    // NFR-1 cascade budget. Defaults to the strict 1s target for local/native runs;
    // in CI the Neo4j Testcontainer is reached over the Docker bridge (host.docker.internal),
    // so the round-trip latency budget is widened via PROMOTION_NFR_CASCADE_MAX_MS.
    private static final long CASCADE_MAX_MS = resolveCascadeBudget();

    private static long resolveCascadeBudget() {
        String override = System.getenv("PROMOTION_NFR_CASCADE_MAX_MS");
        return override != null ? Long.parseLong(override.trim()) : 1000L;
    }

    @BeforeEach
    void setupBenchmarkData() {
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = Mockito.mock(ValueOperations.class);
        Mockito.lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);

        // Clear graph
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();

        // Create 10,000 nodes and random contacts
        rootUser = UUID.randomUUID().toString();
        
        // 1. Create root user
        neo4jClient.query("CREATE (:User {anonymousId: $id, status: 'ACTIVE'})")
                .bind(rootUser).to("id").run();

        // 2. Create 10,000 secondary nodes in batches for performance
        // This is a simplified scale model for benchmarking
        neo4jClient.query("UNWIND range(1, 10000) as i " +
                "CREATE (u:User {anonymousId: 'user-' + toString(i), status: 'ACTIVE'})")
                .run();

        // 3. Connect root to a subset (Realistic average)
        neo4jClient.query("MATCH (root:User {anonymousId: $id}), (others:User) " +
                "WHERE others.anonymousId <> $id " +
                "WITH root, others LIMIT 50 " +
                "CREATE (root)-[:ENCOUNTERED {startTime: timestamp()}]->(others)")
                .bind(rootUser).to("id")
                .run();
                
        // Connect others in a chain/mesh (Realistic density)
        neo4jClient.query("MATCH (u1:User), (u2:User) " +
                "WHERE u1.anonymousId <> u2.anonymousId AND rand() < 0.001 " +
                "WITH u1, u2 LIMIT 15000 " +
                "CREATE (u1)-[:ENCOUNTERED {startTime: timestamp()}]->(u2)")
                .run();
    }

    @Test
    void benchmarkPromotionPerformance() {
        System.out.println("Starting Promotion Benchmark...");
        
        // --- Warmup Phase ---
        // Perform a small promotion to warm up indices and JIT
        String warmupUser = "user-1"; 
        healthStatusService.updateStatus(warmupUser, "CONFIRMED");
        System.out.println("Warmup phase complete.");
        
        // --- Main Benchmark ---
        long startTime = System.currentTimeMillis();
        
        // Trigger promotion on rootUser (affects 10,000 node cluster)
        healthStatusService.updateStatus(rootUser, "CONFIRMED");
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("==========================================");
        System.out.println("TOTAL DURATION: " + duration + "ms");
        System.out.println("==========================================");
        
        // Assert NFR-1 target (configurable budget; strict 1000ms default for native runs)
        assertTrue(duration < CASCADE_MAX_MS,
                "Promotion cascade exceeded NFR-1 budget of " + CASCADE_MAX_MS + "ms. Actual: " + duration + "ms");

        // --- Multi-Tier Validation ---
        // Verify L1 promotion (SUSPECT)
        Long suspectCount = neo4jClient.query("MATCH (root:User {anonymousId: $id})-[:ENCOUNTERED]-(c1:User) " +
                "WHERE c1.status = 'SUSPECT' RETURN count(c1) as count")
                .bind(rootUser).to("id")
                .fetchAs(Long.class).one().get();
        System.out.println("L1 SUSPECT COUNT: " + suspectCount);
        assertTrue(suspectCount > 0, "No L1 contacts were promoted to SUSPECT");

        // Verify L2 promotion (PROBABLE)
        Long probableCount = neo4jClient.query("MATCH (root:User {anonymousId: $id})-[:ENCOUNTERED]-(c1)-[:ENCOUNTERED]-(c2:User) " +
                "WHERE c2.status = 'PROBABLE' AND c2.anonymousId <> root.anonymousId RETURN count(c2) as count")
                .bind(rootUser).to("id")
                .fetchAs(Long.class).one().get();
        System.out.println("L2 PROBABLE COUNT: " + probableCount);
        assertTrue(probableCount > 0, "No L2 contacts were promoted to PROBABLE");
    }
}
