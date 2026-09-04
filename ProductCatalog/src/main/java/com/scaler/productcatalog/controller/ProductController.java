package com.scaler.productcatalog.controller;

import com.scaler.productcatalog.dto.product.ImageCreateRequestDTO;
import com.scaler.productcatalog.dto.product.ImageResponseDTO;
import com.scaler.productcatalog.dto.product.ImageUpdateRequestDTO;
import com.scaler.productcatalog.dto.product.ProductCreateRequestDTO;
import com.scaler.productcatalog.dto.product.ProductResponseDTO;
import com.scaler.productcatalog.dto.product.ProductUpdateRequestDTO;
import com.scaler.productcatalog.service.ProductService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<Page<ProductResponseDTO>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            Pageable pageable) {
        return ResponseEntity.ok(productService.searchActive(query, categoryId, minPrice, maxPrice, pageable));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponseDTO> getById(@PathVariable UUID productId) {
        return ResponseEntity.ok(productService.getActive(productId));
    }

    @PostMapping
    public ResponseEntity<ProductResponseDTO> create(@Valid @RequestBody ProductCreateRequestDTO request) {
        ProductResponseDTO created = productService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/{productId}")
    public ResponseEntity<ProductResponseDTO> update(@PathVariable UUID productId,
                                                     @Valid @RequestBody ProductUpdateRequestDTO request) {
        return ResponseEntity.ok(productService.update(productId, request));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> delete(@PathVariable UUID productId) {
        productService.softDelete(productId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{productId}/images")
    public ResponseEntity<ImageResponseDTO> addImage(@PathVariable UUID productId,
                                                     @Valid @RequestBody ImageCreateRequestDTO request) {
        return ResponseEntity.status(201).body(productService.addImage(productId, request));
    }

    @PatchMapping("/{productId}/images/{imageId}")
    public ResponseEntity<ImageResponseDTO> updateImage(@PathVariable UUID productId,
                                                        @PathVariable UUID imageId,
                                                        @Valid @RequestBody ImageUpdateRequestDTO request) {
        return ResponseEntity.ok(productService.updateImage(productId, imageId, request));
    }

    @DeleteMapping("/{productId}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable UUID productId, @PathVariable UUID imageId) {
        productService.deleteImage(productId, imageId);
        return ResponseEntity.noContent().build();
    }
}