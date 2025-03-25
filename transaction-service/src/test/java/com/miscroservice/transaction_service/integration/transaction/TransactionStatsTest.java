package com.miscroservice.transaction_service.integration.transaction;

import com.miscroservice.transaction_service.integration.BaseIntegrationTest;
import com.miscroservice.transaction_service.model.dto.TransactionRequest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.equalTo;

public class TransactionStatsTest extends BaseIntegrationTest {

    @Test
    void getStats_Success() {
        // Создаем две транзакции
        TransactionRequest incomeRequest = new TransactionRequest();
        incomeRequest.setAmount(new BigDecimal("100.00"));
        incomeRequest.setType("INCOME");
        incomeRequest.setCategory("Salary");
        incomeRequest.setDescription("Test income");
        incomeRequest.setDate(Instant.now().toString());

        TransactionRequest expenseRequest = new TransactionRequest();
        expenseRequest.setAmount(new BigDecimal("50.00"));
        expenseRequest.setType("EXPENSE");
        expenseRequest.setCategory("Groceries");
        expenseRequest.setDescription("Test expense");
        expenseRequest.setDate(Instant.now().toString());

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer mock-token")
                .body(incomeRequest)
                .when()
                .post("/transactions")
                .then()
                .statusCode(201);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer mock-token")
                .body(expenseRequest)
                .when()
                .post("/transactions")
                .then()
                .statusCode(201);

        // Проверяем статистику
        given()
                .header("Authorization", "Bearer mock-token")
                .when()
                .get("/transactions/stats")
                .then()
                .statusCode(200)
                .body("totalIncome", equalTo(100.00f))
                .body("totalExpense", equalTo(50.00f))
                .body("byCategory.Salary", equalTo(100.00f))
                .body("byCategory.Groceries", equalTo(50.00f));
    }

    @Test
    void getStats_WithDateFilter() {
        TransactionRequest oldRequest = new TransactionRequest();
        oldRequest.setAmount(new BigDecimal("200.00"));
        oldRequest.setType("INCOME");
        oldRequest.setCategory("Salary");
        oldRequest.setDescription("Old income");
        oldRequest.setDate("2023-01-01T00:00:00Z");

        TransactionRequest recentRequest = new TransactionRequest();
        recentRequest.setAmount(new BigDecimal("150.00"));
        recentRequest.setType("EXPENSE");
        recentRequest.setCategory("Groceries");
        recentRequest.setDescription("Recent expense");
        recentRequest.setDate("2023-02-01T00:00:00Z");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer mock-token")
                .body(oldRequest)
                .when()
                .post("/transactions")
                .then()
                .statusCode(201);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer mock-token")
                .body(recentRequest)
                .when()
                .post("/transactions")
                .then()
                .statusCode(201);

        given()
                .header("Authorization", "Bearer mock-token")
                .queryParam("startDate", "2023-01-15T00:00:00Z")
                .queryParam("endDate", "2023-02-15T00:00:00Z")
                .when()
                .get("/transactions/stats")
                .then()
                .statusCode(200)
                .body("totalIncome", equalTo(0)) // Нет доходов в этом диапазоне
                .body("totalExpense", equalTo(150.00f))
                .body("byCategory.Groceries", equalTo(150.00f));
    }

    // Новый тест: Статистика без транзакций
    @Test
    void getStats_NoTransactions() {
        given()
                .header("Authorization", "Bearer mock-token")
                .when()
                .get("/transactions/stats")
                .then()
                .statusCode(200)
                .body("totalIncome", equalTo(0))
                .body("totalExpense", equalTo(0))
                .body("byCategory", anEmptyMap());
    }

}
