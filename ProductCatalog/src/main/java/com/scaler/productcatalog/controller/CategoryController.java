package com.scaler.productcatalog.controller;

import com.scaler.productcatalog.dto.category.CategoryCreateRequestDTO;
import com.scaler.productcatalog.dto.category.CategoryResponseDTO;
import com.scaler.productcatalog.dto.category.CategoryUpdateRequestDTO;
import com.scaler.productcatalog.service.CategoryService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryResponseDTO>> listActive() {
        return ResponseEntity.ok(categoryService.listActive());
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryResponseDTO> getById(@PathVariable UUID categoryId) {
        return ResponseEntity.ok(categoryService.get(categoryId));
    }

    @PostMapping
    public ResponseEntity<CategoryResponseDTO> create(@Valid @RequestBody CategoryCreateRequestDTO request) {
        CategoryResponseDTO created = categoryService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/{categoryId}")
    public ResponseEntity<CategoryResponseDTO> update(@PathVariable UUID categoryId,
                                                      @Valid @RequestBody CategoryUpdateRequestDTO request) {
        return ResponseEntity.ok(categoryService.update(categoryId, request));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID categoryId) {
        categoryService.deactivate(categoryId);
        return ResponseEntity.noContent().build();
    }
}
