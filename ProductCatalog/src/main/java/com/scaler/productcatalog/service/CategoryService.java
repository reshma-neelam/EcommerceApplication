package com.scaler.productcatalog.service;

import com.scaler.productcatalog.dto.category.CategoryCreateRequestDTO;
import com.scaler.productcatalog.dto.category.CategoryResponseDTO;
import com.scaler.productcatalog.dto.category.CategoryUpdateRequestDTO;
import java.util.List;
import java.util.UUID;

public interface CategoryService {

    List<CategoryResponseDTO> listActive();

    CategoryResponseDTO get(UUID id);

    CategoryResponseDTO create(CategoryCreateRequestDTO request);

    CategoryResponseDTO update(UUID id, CategoryUpdateRequestDTO request);

    void deactivate(UUID id);
}
