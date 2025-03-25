package com.miscroservice.transaction_service.integration.transaction;

import com.miscroservice.transaction_service.integration.BaseIntegrationTest;
import com.miscroservice.transaction_service.model.dto.TransactionRequest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class TransactionDeletionTest extends BaseIntegrationTest {

    @Test
    void deleteTransaction_Success() {
        // Создаем транзакцию
        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("100.00"));
        request.setType("INCOME");
        request.setCategory("Salary");
        request.setDescription("Test transaction");
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

        // Удаляем транзакцию
        given()
                .header("Authorization", "Bearer mock-token")
                .when()
                .delete("/transactions/" + transactionId)
                .then()
                .statusCode(204); // No Content

        // Проверяем, что транзакция больше не доступна (опционально)
        given()
                .header("Authorization", "Bearer mock-token")
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/transactions")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(0));
    }

    @Test
    void deleteTransaction_NotFound() {
        given()
                .header("Authorization", "Bearer mock-token")
                .when()
                .delete("/transactions/" + UUID.randomUUID())
                .then()
                .statusCode(404)
                .body(containsString("Transaction not found"));
    }

}
