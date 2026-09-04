package com.scaler.productcatalog.dto.product;

import com.scaler.productcatalog.enums.ProductStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponseDTO {
    private UUID id;
    private String sku;
    private String name;
    private String description;
    private String brand;
    private ProductStatus status;
    private BigDecimal basePrice;
    private String currency;
    private List<UUID> categoryIds;
    private UUID primaryCategoryId;
    private List<ImageResponseDTO> images;
    private Integer availableQuantity;
    private long version;
    private Instant createdAt;
    private Instant updatedAt;
}
