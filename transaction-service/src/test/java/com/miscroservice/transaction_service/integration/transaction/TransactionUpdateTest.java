package com.miscroservice.transaction_service.integration.transaction;

import com.miscroservice.transaction_service.integration.BaseIntegrationTest;
import com.miscroservice.transaction_service.model.dto.TransactionRequest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class TransactionUpdateTest extends BaseIntegrationTest {

    @Test
    void updateTransaction_Success() {
        // Создаем транзакцию
        TransactionRequest createRequest = new TransactionRequest();
        createRequest.setAmount(new BigDecimal("100.00"));
        createRequest.setType("INCOME"); // Убедимся, что тип в верхнем регистре
        createRequest.setCategory("Salary");
        createRequest.setDescription("Initial transaction");
        createRequest.setDate(Instant.now().toString());
        createRequest.setPaymentMethod("Credit Cart");

        String transactionId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer mock-token")
                .body(createRequest)
                .when()
                .post("/transactions")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Обновляем транзакцию
        TransactionRequest updateRequest = new TransactionRequest();
        updateRequest.setAmount(new BigDecimal("150.00"));
        updateRequest.setType("EXPENSE"); // Убедимся, что тип в верхнем регистре
        updateRequest.setCategory("Groceries");
        updateRequest.setDescription("Updated transaction");
        updateRequest.setDate(Instant.now().toString());


        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer mock-token")
                .body(updateRequest)
                .when()
                .put("/transactions/" + transactionId)
                .then()
                .statusCode(200)
                .body("id", equalTo(transactionId))
                .body("amount", equalTo(150.00f))
                .body("type", equalTo("EXPENSE"))
                .body("category", equalTo("Groceries"))
                .body("description", equalTo("Updated transaction"));
    }

    @Test
    void updateTransaction_Forbidden() {
        UUID anotherUserId = UUID.randomUUID();
        String anotherUserToken = "mock-token-another-user";
        Mockito.when(jwtUtil.validateToken(anotherUserToken)).thenReturn(true);
        Mockito.when(jwtUtil.extractUserId(anotherUserToken)).thenReturn(anotherUserId.toString());

        TransactionRequest createRequest = new TransactionRequest();
        createRequest.setAmount(new BigDecimal("100.00"));
        createRequest.setType("INCOME");
        createRequest.setCategory("Salary");
        createRequest.setDescription("Another user's transaction");
        createRequest.setDate(Instant.now().toString());

        String transactionId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + anotherUserToken)
                .body(createRequest)
                .when()
                .post("/transactions")
                .then()
                .statusCode(201)
                .extract().path("id");

        String originalUserToken = "mock-token";
        Mockito.when(jwtUtil.validateToken(originalUserToken)).thenReturn(true);
        Mockito.when(jwtUtil.extractUserId(originalUserToken)).thenReturn(userId.toString());

        TransactionRequest updateRequest = new TransactionRequest();
        updateRequest.setAmount(new BigDecimal("150.00"));
        updateRequest.setType("EXPENSE");
        updateRequest.setCategory("Groceries");
        updateRequest.setDescription("Updated transaction");
        updateRequest.setDate(Instant.now().toString());

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + originalUserToken)
                .body(updateRequest)
                .when()
                .put("/transactions/" + transactionId)
                .then()
                .statusCode(403);
    }

    @Test
    void updateTransaction_NonExistentCategory() {
        TransactionRequest createRequest = new TransactionRequest();
        createRequest.setAmount(new BigDecimal("100.00"));
        createRequest.setType("INCOME");
        createRequest.setCategory("Salary");
        createRequest.setDescription("Initial transaction");
        createRequest.setDate(Instant.now().toString());

        String transactionId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer mock-token")
                .body(createRequest)
                .when()
                .post("/transactions")
                .then()
                .statusCode(201)
                .extract().path("id");

        TransactionRequest updateRequest = new TransactionRequest();
        updateRequest.setAmount(new BigDecimal("150.00"));
        updateRequest.setType("EXPENSE");
        updateRequest.setCategory("NonExistentCategory");
        updateRequest.setDescription("Updated transaction");
        updateRequest.setDate(Instant.now().toString());

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer mock-token")
                .body(updateRequest)
                .when()
                .put("/transactions/" + transactionId)
                .then()
                .statusCode(400)
                .body(containsString("Category 'NonExistentCategory' does not exist"));
    }

    // Новый тест: Обновление несуществующей транзакции
    @Test
    void updateTransaction_NotFound() {
        TransactionRequest updateRequest = new TransactionRequest();
        updateRequest.setAmount(new BigDecimal("150.00"));
        updateRequest.setType("EXPENSE");
        updateRequest.setCategory("Groceries");
        updateRequest.setDescription("Updated transaction");
        updateRequest.setDate(Instant.now().toString());

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer mock-token")
                .body(updateRequest)
                .when()
                .put("/transactions/" + UUID.randomUUID())
                .then()
                .statusCode(404)
                .body(containsString("Transaction not found"));
    }
}
