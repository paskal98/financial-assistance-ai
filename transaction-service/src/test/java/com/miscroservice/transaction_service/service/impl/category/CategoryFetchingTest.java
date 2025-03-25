package com.miscroservice.transaction_service.service.impl.category;

import com.miscroservice.transaction_service.model.dto.CategoryResponse;
import com.miscroservice.transaction_service.model.entity.Category;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

public class CategoryFetchingTest extends BaseCategoryTest {

    @Test
    void getCategories_AllCategories_Success() {
        // Arrange
        List<Category> categories = List.of(category);
        List<CategoryResponse> expectedResponses = List.of(categoryResponse);
        String cacheKey = CATEGORIES_CACHE_PREFIX + "all";
        when(valueOperations.get(cacheKey)).thenReturn(null);
        when(categoryRepository.findAll()).thenReturn(categories);
        doNothing().when(valueOperations).set(anyString(), any(), anyLong(), any());

        // Act
        List<CategoryResponse> result = categoryService.getCategories(null);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponses, result);
        verify(valueOperations).get(cacheKey);
        verify(categoryRepository).findAll();
        verify(valueOperations).set(cacheKey, expectedResponses, 1, TimeUnit.HOURS);
    }

    @Test
    void getCategories_ByType_Success() {
        // Arrange
        List<Category> categories = List.of(category);
        List<CategoryResponse> expectedResponses = List.of(categoryResponse);
        String type = "INCOME";
        String cacheKey = CATEGORIES_CACHE_PREFIX + type;
        when(valueOperations.get(cacheKey)).thenReturn(null);
        when(categoryRepository.findAllByType(type)).thenReturn(categories);
        doNothing().when(valueOperations).set(anyString(), any(), anyLong(), any());

        // Act
        List<CategoryResponse> result = categoryService.getCategories(type);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponses, result);
        verify(valueOperations).get(cacheKey);
        verify(categoryRepository).findAllByType(type);
        verify(valueOperations).set(cacheKey, expectedResponses, 1, TimeUnit.HOURS);
    }

    @Test
    void getCategories_EmptyResult() {
        // Arrange
        String cacheKey = CATEGORIES_CACHE_PREFIX + "all";
        when(valueOperations.get(cacheKey)).thenReturn(null);
        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<CategoryResponse> result = categoryService.getCategories(null);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
        verify(valueOperations).get(cacheKey);
        verify(categoryRepository).findAll();
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any());
    }

    @Test
    void getCategories_RedisFailure_SuccessFromDB() {
        // Arrange
        List<Category> categories = List.of(category);
        List<CategoryResponse> expectedResponses = List.of(categoryResponse);
        String cacheKey = CATEGORIES_CACHE_PREFIX + "all";
        when(valueOperations.get(cacheKey)).thenThrow(new RuntimeException("Redis unavailable"));
        when(categoryRepository.findAll()).thenReturn(categories);

        // Act
        List<CategoryResponse> result = categoryService.getCategories(null);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponses, result);
        verify(valueOperations).get(cacheKey);
        verify(categoryRepository).findAll();
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any());
    }

    @Test
    void getCategories_DatabaseFailure_ReturnsEmptyList() {
        String cacheKey = CATEGORIES_CACHE_PREFIX + "all";
        when(valueOperations.get(cacheKey)).thenReturn(null);
        when(categoryRepository.findAll()).thenThrow(new RuntimeException("DB unavailable"));

        List<CategoryResponse> result = categoryService.getCategories(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(valueOperations).get(cacheKey);
        verify(categoryRepository).findAll();
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any());
    }
}