package com.miscroservice.transaction_service.service.impl.category;

import com.miscroservice.transaction_service.model.dto.CategoryResponse;
import com.miscroservice.transaction_service.model.entity.Category;
import com.miscroservice.transaction_service.repository.CategoryRepository;
import com.miscroservice.transaction_service.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

abstract class BaseCategoryTest {

    @Mock
    protected CategoryRepository categoryRepository;
    @Mock
    protected RedisTemplate<String, Object> redisTemplate;
    @Mock
    protected ValueOperations<String, Object> valueOperations;

    protected CategoryServiceImpl categoryService;
    protected Category category;
    protected CategoryResponse categoryResponse;
    protected static final String CATEGORIES_CACHE_PREFIX = "categories:";

    @BeforeEach
    void baseSetUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations); // Настраиваем ValueOperations
        categoryService = new CategoryServiceImpl(categoryRepository, redisTemplate);

        category = new Category();
        category.setId(1);
        category.setName("Salary");
        category.setType("INCOME");

        categoryResponse = new CategoryResponse(category.getId(), category.getName(), category.getType());
    }
}