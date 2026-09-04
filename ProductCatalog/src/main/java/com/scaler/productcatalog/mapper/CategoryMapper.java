package com.scaler.productcatalog.mapper;

import com.scaler.productcatalog.dto.category.CategoryResponseDTO;
import com.scaler.productcatalog.model.Category;

public final class CategoryMapper {

    private CategoryMapper() {
    }

    public static CategoryResponseDTO toResponse(Category category) {
        return CategoryResponseDTO.builder()
                .id(category.getId())
                .parentCategoryId(category.getParentCategoryId())
                .name(category.getName())
                .slug(category.getSlug())
                .status(category.getStatus())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
