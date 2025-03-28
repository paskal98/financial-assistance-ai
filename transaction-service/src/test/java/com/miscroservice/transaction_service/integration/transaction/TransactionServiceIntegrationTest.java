package com.miscroservice.transaction_service.integration.transaction;

import com.miscroservice.transaction_service.integration.BaseIntegrationTest;
import com.miscroservice.transaction_service.model.dto.TransactionItemDto;
import com.miscroservice.transaction_service.model.dto.TransactionRequest;
import com.miscroservice.transaction_service.model.dto.TransactionResponse;
import com.miscroservice.transaction_service.repository.TransactionRepository;
import com.miscroservice.transaction_service.service.TransactionService;
import io.restassured.http.ContentType;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TransactionServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionRepository transactionRepository;

    // Настройка Kafka Producer для тестов
    private KafkaTemplate<String, String> createKafkaProducer(KafkaContainer kafka) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(configProps));
    }

    @Test
    void testTransactionCreationViaKafka_Success() throws InterruptedException {
        TransactionItemDto item = new TransactionItemDto();
        item.setName("Kafka Transaction");
        item.setCategory("Salary");
        item.setType("INCOME");
        item.setPrice(new BigDecimal("300.00"));
        item.setDate(Instant.now());
        item.setUserId(userId);
        item.setDocumentId(UUID.randomUUID());

        String message = "{\"name\":\"Kafka Transaction\",\"category\":\"Salary\",\"type\":\"INCOME\"," +
                "\"price\":300.00,\"date\":\"" + item.getDate().toString() + "\",\"userId\":\"" + userId + "\"," +
                "\"documentId\":\"" + item.getDocumentId() + "\"}";

        KafkaTemplate<String, String> kafkaProducer = createKafkaProducer(kafka);
        kafkaProducer.send("transactions-topic", message);

        Thread.sleep(5000); // Увеличиваем время ожидания

        var transactions = transactionRepository.findByUserId(userId);
        assertEquals(1, transactions.size());
        assertEquals("Kafka Transaction", transactions.get(0).getDescription());
        assertEquals(item.getDocumentId(), transactions.get(0).getDocumentId());
    }

    @Test
    void testTransactionCreationViaAPI_Success() {
        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("200.00"));
        request.setType("INCOME");
        request.setCategory("Salary");
        request.setDescription("API Transaction");
        request.setDate(Instant.now().toString());

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer mock-token")
                .body(request)
                .when()
                .post("/transactions")
                .then()
                .statusCode(201)
                .body("amount", equalTo(200.00f))
                .body("description", equalTo("API Transaction"));
    }

    @Test
    void testRedisCacheInvalidation() throws InterruptedException {
        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("150.00"));
        request.setType("EXPENSE");
        request.setCategory("Groceries");
        request.setDescription("Cached Transaction");
        request.setDate(Instant.now().toString());

        String transactionId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer mock-token")
                .body(request)
                .when()
                .post("/transactions")
                .then()
                .statusCode(201)
                .extract().path("id");

        Page<TransactionResponse> firstCall = transactionService.getTransactions(userId, null, null, null, null, PageRequest.of(0, 10));
        assertEquals(1, firstCall.getTotalElements());

        given()
                .header("Authorization", "Bearer mock-token")
                .when()
                .delete("/transactions/" + transactionId)
                .then()
                .statusCode(204);

        Thread.sleep(2000); // Увеличиваем ожидание

        Page<TransactionResponse> secondCall = transactionService.getTransactions(userId, null, null, null, null, PageRequest.of(0, 10));
        assertEquals(0, secondCall.getTotalElements());
    }

    @Test
    void testKafkaFailure_StillProcessesViaAPI() {
        kafka.stop();

        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("400.00"));
        request.setType("INCOME");
        request.setCategory("Salary");
        request.setDescription("API Transaction without Kafka");
        request.setDate(Instant.now().toString());

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer mock-token")
                .body(request)
                .when()
                .post("/transactions")
                .then()
                .statusCode(201)
                .body("amount", equalTo(400.00f));

        var transactions = transactionRepository.findByUserId(userId);
        assertEquals(1, transactions.size());

        kafka.start();
    }

    @Test
    void testRedisFailure_FallbackToDatabase() {
        // Arrange: Останавливаем Redis
        redis.stop();

        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("500.00"));
        request.setType("EXPENSE");
        request.setCategory("Groceries");
        request.setDescription("No Redis Transaction");
        request.setDate(Instant.now().toString());

        // Act: Создаем транзакцию без Redis
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer mock-token")
                .body(request)
                .when()
                .post("/transactions")
                .then()
                .statusCode(201);

        // Assert: Проверяем через сервис, что данные попали в БД
        Page<TransactionResponse> transactions = transactionService.getTransactions(userId, null, null, null, null, PageRequest.of(0, 10));
        assertEquals(1, transactions.getTotalElements());
        assertEquals("No Redis Transaction", transactions.getContent().get(0).getDescription());

        // Восстанавливаем Redis
        redis.start();
    }

    // Переопределяем конфигурацию Testcontainers, если нужно
    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}