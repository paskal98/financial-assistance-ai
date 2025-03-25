package com.miscroservice.transaction_service.integration;

import com.miscroservice.transaction_service.repository.TransactionRepository;
import com.miscroservice.transaction_service.security.JwtUtil;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public abstract class BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @MockBean
    protected JwtUtil jwtUtil;

    protected UUID userId;

    @Autowired
    private TransactionRepository transactionRepository;

    @Container
    protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("transactions_db")
            .withUsername("postgres")
            .withPassword("postgres")
            .withInitScript("schema.sql");

    @Container
    protected static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.3.0"));

    @Container
    protected static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.0"))
            .withExposedPorts(6379);

    @BeforeAll
    static void startContainers() {
        postgres.start();
        kafka.start();
        redis.start();
        if (!postgres.isRunning()) {
            throw new IllegalStateException("PostgreSQL container failed to start");
        }
        System.out.println("PostgreSQL JDBC URL: " + postgres.getJdbcUrl());
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        userId = UUID.randomUUID();

        if (!postgres.isRunning()) {
            System.err.println("PostgreSQL container is not running. Restarting...");
            postgres.start();
            if (!postgres.isRunning()) {
                throw new IllegalStateException("Failed to restart PostgreSQL container");
            }
            System.out.println("PostgreSQL restarted. JDBC URL: " + postgres.getJdbcUrl());
        }

        Mockito.when(jwtUtil.validateToken("mock-token")).thenReturn(true);
        Mockito.when(jwtUtil.extractUserId("mock-token")).thenReturn(userId.toString());

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId.toString(), null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            transactionRepository.deleteAll();
        } catch (Exception e) {
            System.err.println("Failed to clear database: " + e.getMessage());
            throw new RuntimeException("Database cleanup failed", e);
        }
    }

    @AfterEach
    void tearDown() {
        try {
            transactionRepository.deleteAll();
        } catch (Exception e) {
            System.err.println("Failed to clear database in tearDown: " + e.getMessage());
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
    }
}