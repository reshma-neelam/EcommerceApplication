package com.scaler.productcatalog.service;

import com.scaler.productcatalog.dto.category.CategoryCreateRequestDTO;
import com.scaler.productcatalog.dto.category.CategoryResponseDTO;
import com.scaler.productcatalog.dto.category.CategoryUpdateRequestDTO;
import com.scaler.productcatalog.enums.CategoryStatus;
import com.scaler.productcatalog.exception.BadRequestException;
import com.scaler.productcatalog.exception.ConflictException;
import com.scaler.productcatalog.exception.NotFoundException;
import com.scaler.productcatalog.mapper.CategoryMapper;
import com.scaler.productcatalog.model.Category;
import com.scaler.productcatalog.repository.CategoryRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> listActive() {
        return categoryRepository.findByStatus(CategoryStatus.ACTIVE).stream()
                .map(CategoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponseDTO get(UUID id) {
        return CategoryMapper.toResponse(require(id));
    }

    @Override
    @Transactional
    public CategoryResponseDTO create(CategoryCreateRequestDTO request) {
        if (categoryRepository.existsBySlug(request.getSlug())) {
            throw new ConflictException("DUPLICATE_SLUG", "Category slug already exists: " + request.getSlug());
        }
        if (request.getParentCategoryId() != null && !categoryRepository.existsById(request.getParentCategoryId())) {
            throw new BadRequestException("INVALID_PARENT", "Parent category does not exist");
        }
        Category category = new Category();
        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setParentCategoryId(request.getParentCategoryId());
        category.setStatus(CategoryStatus.ACTIVE);
        return CategoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponseDTO update(UUID id, CategoryUpdateRequestDTO request) {
        Category category = require(id);
        if (request.getSlug() != null && !request.getSlug().equals(category.getSlug())
                && categoryRepository.existsBySlug(request.getSlug())) {
            throw new ConflictException("DUPLICATE_SLUG", "Category slug already exists: " + request.getSlug());
        }
        if (request.getParentCategoryId() != null) {
            if (request.getParentCategoryId().equals(id)) {
                throw new BadRequestException("INVALID_PARENT", "Category cannot be its own parent");
            }
            if (!categoryRepository.existsById(request.getParentCategoryId())) {
                throw new BadRequestException("INVALID_PARENT", "Parent category does not exist");
            }
            category.setParentCategoryId(request.getParentCategoryId());
        }
        if (request.getName() != null) {
            category.setName(request.getName());
        }
        if (request.getSlug() != null) {
            category.setSlug(request.getSlug());
        }
        if (request.getStatus() != null) {
            category.setStatus(request.getStatus());
        }
        return CategoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void deactivate(UUID id) {
        Category category = require(id);
        category.setStatus(CategoryStatus.INACTIVE);
        categoryRepository.save(category);
    }

    private Category require(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "Category not found: " + id));
    }
}
