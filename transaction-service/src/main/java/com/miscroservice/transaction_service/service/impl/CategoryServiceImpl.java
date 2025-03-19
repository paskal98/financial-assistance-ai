package com.miscroservice.transaction_service.service.impl;

import com.miscroservice.transaction_service.model.dto.CategoryResponse;
import com.miscroservice.transaction_service.repository.CategoryRepository;
import com.miscroservice.transaction_service.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CATEGORIES_CACHE_PREFIX = "categories:";

    @Override
    public List<CategoryResponse> getCategories(String type) {
        String cacheKey = CATEGORIES_CACHE_PREFIX + (type != null ? type : "all");
        @SuppressWarnings("unchecked")
        List<CategoryResponse> cachedCategories = (List<CategoryResponse>) redisTemplate.opsForValue().get(cacheKey);

        if (cachedCategories != null) {
            return cachedCategories;
        }

        List<CategoryResponse> categories;
        if (type != null) {
            categories = categoryRepository.findAllByType(type).stream()
                    .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getType()))
                    .toList();
        } else {
            categories = categoryRepository.findAll().stream()
                    .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getType()))
                    .toList();
        }

        redisTemplate.opsForValue().set(cacheKey, categories, 1, TimeUnit.HOURS); // Кэш на час
        return categories;
    }
}
