package com.scaler.productcatalog.mapper;

import com.scaler.productcatalog.dto.product.ImageResponseDTO;
import com.scaler.productcatalog.dto.product.ProductResponseDTO;
import com.scaler.productcatalog.model.Product;
import com.scaler.productcatalog.model.ProductImage;
import java.util.List;
import java.util.UUID;

public final class ProductMapper {

    private ProductMapper() {
    }

    public static ProductResponseDTO toResponse(Product product, List<UUID> categoryIds, UUID primaryCategoryId,
                                                List<ProductImage> images, Integer availableQuantity) {
        List<ImageResponseDTO> imageResponses = images.stream().map(ProductMapper::toImageResponse).toList();
        return ProductResponseDTO.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .brand(product.getBrand())
                .status(product.getStatus())
                .basePrice(product.getBasePrice())
                .currency(product.getCurrency())
                .categoryIds(categoryIds)
                .primaryCategoryId(primaryCategoryId)
                .images(imageResponses)
                .availableQuantity(availableQuantity)
                .version(product.getVersion())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    public static ImageResponseDTO toImageResponse(ProductImage image) {
        return ImageResponseDTO.builder()
                .id(image.getId())
                .productId(image.getProductId())
                .url(image.getUrl())
                .altText(image.getAltText())
                .displayOrder(image.getDisplayOrder())
                .primary(image.isPrimary())
                .createdAt(image.getCreatedAt())
                .build();
    }
}
