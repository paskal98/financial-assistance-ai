package com.miscroservice.transaction_service.repository;

import com.miscroservice.transaction_service.model.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    Optional<Category> findByName(String name);
    List<Category> findAllByType(String type);
}