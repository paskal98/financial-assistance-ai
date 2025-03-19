package com.miscroservice.transaction_service.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CategoryResponse {
    private Integer id;
    private String name;
    private String type;

    public CategoryResponse(Integer id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }
}