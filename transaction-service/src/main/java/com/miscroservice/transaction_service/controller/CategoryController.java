package com.miscroservice.transaction_service.controller;

import com.miscroservice.transaction_service.model.dto.CategoryResponse;
import com.miscroservice.transaction_service.service.CategoryService;
import com.miscroservice.transaction_service.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getCategories(
            @RequestParam(required = false) String type) {
        List<CategoryResponse> categories = categoryService.getCategories(type);
        return ResponseEntity.ok(categories);
    }
}