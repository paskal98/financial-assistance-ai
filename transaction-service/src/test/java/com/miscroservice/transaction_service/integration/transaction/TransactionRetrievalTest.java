package com.miscroservice.transaction_service.integration.transaction;

import com.miscroservice.transaction_service.integration.BaseIntegrationTest;
import com.miscroservice.transaction_service.model.dto.TransactionRequest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

public class TransactionRetrievalTest extends BaseIntegrationTest {

    @Test
    void getTransactions_Success() {
        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("100.00"));
        request.setType("INCOME");
        request.setCategory("Salary");
        request.setDescription("Test transaction");
        request.setDate(Instant.now().toString());

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer mock-token")
                .body(request)
                .when()
                .post("/transactions")
                .then()
                .statusCode(201);

        given()
                .header("Authorization", "Bearer mock-token")
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/transactions")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThan(0))
                .body("content[0].category", equalTo("Salary"));
    }

    @Test
    void getTransactions_WithFilters() {
        // Создаем несколько транзакций
        TransactionRequest incomeRequest = new TransactionRequest();
        incomeRequest.setAmount(new BigDecimal("100.00"));
        incomeRequest.setType("INCOME");
        incomeRequest.setCategory("Salary");
        incomeRequest.setDescription("Income transaction");
        incomeRequest.setDate("2023-01-01T00:00:00Z");

        TransactionRequest expenseRequest = new TransactionRequest();
        expenseRequest.setAmount(new BigDecimal("50.00"));
        expenseRequest.setType("EXPENSE");
        expenseRequest.setCategory("Groceries");
        expenseRequest.setDescription("Expense transaction");
        expenseRequest.setDate("2023-01-02T00:00:00Z");

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

        // Проверяем фильтрацию по типу и пагинацию
        given()
                .header("Authorization", "Bearer mock-token")
                .queryParam("type", "INCOME")
                .queryParam("page", 0)
                .queryParam("size", 1)
                .when()
                .get("/transactions")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].type", equalTo("INCOME"))
                .body("totalElements", equalTo(1));
    }

    // Новый тест: Проверка фильтрации по датам
    @Test
    void getTransactions_FilterByDateRange() {
        TransactionRequest oldRequest = new TransactionRequest();
        oldRequest.setAmount(new BigDecimal("200.00"));
        oldRequest.setType("INCOME");
        oldRequest.setCategory("Salary");
        oldRequest.setDescription("Old transaction");
        oldRequest.setDate("2023-01-01T00:00:00Z");

        TransactionRequest recentRequest = new TransactionRequest();
        recentRequest.setAmount(new BigDecimal("150.00"));
        recentRequest.setType("EXPENSE");
        recentRequest.setCategory("Groceries");
        recentRequest.setDescription("Recent transaction");
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
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/transactions")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].description", equalTo("Recent transaction"));
    }

    // Новый тест: Проверка пустого результата
    @Test
    void getTransactions_NoTransactions() {
        given()
                .header("Authorization", "Bearer mock-token")
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/transactions")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(0))
                .body("totalElements", equalTo(0));
    }

    // Новый тест: Проверка пагинации
    @Test
    void getTransactions_Pagination() {
        for (int i = 0; i < 15; i++) {
            TransactionRequest request = new TransactionRequest();
            request.setAmount(new BigDecimal("10.00"));
            request.setType("EXPENSE");
            request.setCategory("Groceries");
            request.setDescription("Transaction " + i);
            request.setDate(Instant.now().toString());

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer mock-token")
                    .body(request)
                    .when()
                    .post("/transactions")
                    .then()
                    .statusCode(201);
        }

        given()
                .header("Authorization", "Bearer mock-token")
                .queryParam("page", 1)
                .queryParam("size", 10)
                .when()
                .get("/transactions")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(5)) // Вторая страница: 15 - 10 = 5
                .body("totalElements", equalTo(15))
                .body("totalPages", equalTo(2));
    }
}
