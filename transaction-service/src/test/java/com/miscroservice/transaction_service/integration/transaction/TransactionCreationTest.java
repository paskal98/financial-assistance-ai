package com.miscroservice.transaction_service.integration.transaction;

import com.miscroservice.transaction_service.integration.BaseIntegrationTest;
import com.miscroservice.transaction_service.model.dto.TransactionRequest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class TransactionCreationTest extends BaseIntegrationTest {

    @Test
    void createTransaction_Success() {
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
                .statusCode(201)
                .body("id", notNullValue())
                .body("amount", equalTo(100.00f))
                .body("type", equalTo("INCOME"))
                .body("category", equalTo("Salary"));
    }

    @Test
    void createTransaction_InvalidData() {
        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("-100.00")); // Отрицательная сумма
        request.setType("INVALID_TYPE"); // Неверный тип
        request.setCategory("Salary");
        request.setDescription("Invalid transaction");
        request.setDate(Instant.now().toString());

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer mock-token")
                .body(request)
                .when()
                .post("/transactions")
                .then()
                .statusCode(400) // Bad Request
                .body(containsString("Type must be either 'INCOME' or 'EXPENSE'"));
    }

    @Test
    void createTransaction_Unauthorized() {
        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("100.00"));
        request.setType("INCOME");
        request.setCategory("Salary");
        request.setDescription("Test transaction");
        request.setDate(Instant.now().toString());

        SecurityContextHolder.clearContext();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/transactions")
                .then()
                .statusCode(401); // Unauthorized
    }

    @Test
    void createTransaction_NonExistentCategory() {
        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("100.00"));
        request.setType("INCOME");
        request.setCategory("NonExistentCategory"); // Несуществующая категория
        request.setDescription("Test transaction");
        request.setDate(Instant.now().toString());

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer mock-token")
                .body(request)
                .when()
                .post("/transactions")
                .then()
                .statusCode(400)
                .body(containsString("Category 'NonExistentCategory' does not exist"));
    }

    // Новый тест: Проверка обязательных полей
    @Test
    void createTransaction_MissingRequiredFields() {
        TransactionRequest request = new TransactionRequest();
        // Пропущены обязательные поля: amount, type, category, date
        request.setDescription("Test transaction");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer mock-token")
                .body(request)
                .when()
                .post("/transactions")
                .then()
                .statusCode(400)
                .body(containsString("amount: Amount cannot be null"))
                .body(containsString("type: Type cannot be blank"))
                .body(containsString("category: Category cannot be blank"))
                .body(containsString("date: Date cannot be blank"));
    }

}
