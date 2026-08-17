package com.scaler.productcatalog.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductRequestDto {
    @NotBlank(message = "name is required")
    private String name;

    private String description;

    @NotNull(message = "price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "price must be > 0")
    private BigDecimal price;

    private String imageUrl;

    @NotNull(message = "stockQuantity is required")
    @Min(value = 0, message = "stockQuantity must be >= 0")
    private Integer stockQuantity;

    // As per decision, use categoryName and create on the fly if missing
    @NotBlank(message = "categoryName is required")
    private String categoryName;

    // Optional, carried over to BaseModel.catalogName
    private String catalogName;
}
