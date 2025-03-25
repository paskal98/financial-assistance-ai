package com.miscroservice.transaction_service.service.impl.category;

import com.miscroservice.transaction_service.model.dto.CategoryResponse;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class CategoryCacheTest extends BaseCategoryTest {

    @Test
    void getCategories_FromCache_Success() {
        // Arrange
        List<CategoryResponse> cachedCategories = List.of(categoryResponse);
        String cacheKey = CATEGORIES_CACHE_PREFIX + "all";
        when(valueOperations.get(cacheKey)).thenReturn(cachedCategories);

        // Act
        List<CategoryResponse> result = categoryService.getCategories(null);

        // Assert
        assertEquals(cachedCategories, result);
        verify(valueOperations).get(cacheKey);
        verify(categoryRepository, never()).findAll();
    }

    @Test
    void getCategories_FromCache_WithType_Success() {
        // Arrange
        List<CategoryResponse> cachedCategories = List.of(categoryResponse);
        String type = "INCOME";
        String cacheKey = CATEGORIES_CACHE_PREFIX + type;
        when(valueOperations.get(cacheKey)).thenReturn(cachedCategories);

        // Act
        List<CategoryResponse> result = categoryService.getCategories(type);

        // Assert
        assertEquals(cachedCategories, result);
        verify(valueOperations).get(cacheKey);
        verify(categoryRepository, never()).findAllByType(type);
    }

    @Test
    void getCategories_CacheMiss_NoDataInCache() {
        // Arrange
        String cacheKey = CATEGORIES_CACHE_PREFIX + "all";
        when(valueOperations.get(cacheKey)).thenReturn(null);
        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<CategoryResponse> result = categoryService.getCategories(null);

        // Assert
        assertEquals(Collections.emptyList(), result);
        verify(valueOperations).get(cacheKey);
        verify(categoryRepository).findAll();
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any());
    }
}