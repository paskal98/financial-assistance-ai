package com.miscroservice.transaction_service.integration.category;

import com.miscroservice.transaction_service.model.entity.Category;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class CategoryFetchingTest extends BaseCategoryIntegrationTest {

    @Test
    void getCategories_Success() {
        // Arrange: Добавляем категорию в БД
        Category category = new Category();
        category.setName("Salary");
        category.setType("INCOME");
        categoryRepository.save(category);

        // Act & Assert
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer mock-token")
                .when()
                .get("/categories")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].name", equalTo("Salary"))
                .body("[0].type", equalTo("INCOME"));
    }

    @Test
    void getCategories_WithTypeFilter_Success() {
        // Arrange: Добавляем две категории
        Category incomeCategory = new Category();
        incomeCategory.setName("Salary");
        incomeCategory.setType("INCOME");
        categoryRepository.save(incomeCategory);

        Category expenseCategory = new Category();
        expenseCategory.setName("Groceries");
        expenseCategory.setType("EXPENSE");
        categoryRepository.save(expenseCategory);

        // Act & Assert: Фильтр по типу INCOME
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer mock-token")
                .queryParam("type", "INCOME")
                .when()
                .get("/categories")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].name", equalTo("Salary"))
                .body("[0].type", equalTo("INCOME"));
    }

    @Test
    void getCategories_Unauthorized() {
        SecurityContextHolder.clearContext();

        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/categories")
                .then()
                .statusCode(401);
    }
}