package com.miscroservice.transaction_service.service;

import com.miscroservice.transaction_service.model.dto.CategoryResponse;

import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getCategories(String type);
}
