package com.microservice.document_processing_service.integration;

import com.microservice.document_processing_service.repository.DocumentRepository;
import com.microservice.document_processing_service.security.JwtUtil;
import io.minio.MinioClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import io.restassured.RestAssured;

import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    protected DocumentRepository documentRepository;

    @Autowired
    protected MinioClient minioClient;

    @MockBean
    protected JwtUtil jwtUtil;

    protected UUID userId;

    @Container
    protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("document_db")
            .withUsername("postgres")
            .withPassword("postgres")
            .withInitScript("schema.sql");

    @Container
    protected static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.3.0"));

    @Container
    protected static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.0"))
            .withExposedPorts(6379);

    @Container
    protected static final GenericContainer<?> minio = new GenericContainer<>(DockerImageName.parse("minio/minio"))
            .withExposedPorts(9000)
            .withCommand("server /data")
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin");

    @BeforeAll
    static void startContainers() {
        postgres.start();
        kafka.start();
        redis.start();
        minio.start();
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        userId = UUID.randomUUID();

        // Мокаем JwtUtil
        Mockito.when(jwtUtil.validateToken("mock-token")).thenReturn(true);
        Mockito.when(jwtUtil.extractUserId("mock-token")).thenReturn(userId.toString());

        // Принудительно устанавливаем контекст безопасности
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId.toString(), null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Очистка базы перед тестом
        documentRepository.deleteAll();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("minio.endpoint", () -> "http://" + minio.getHost() + ":" + minio.getFirstMappedPort());
        registry.add("minio.access-key", () -> "minioadmin");
        registry.add("minio.secret-key", () -> "minioadmin");
    }

    @AfterEach
    void tearDown() {
        documentRepository.deleteAll();
    }
}