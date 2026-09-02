package com.scaler.productcatalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductUpdateDto {
    private String name;
    private String description;

    @DecimalMin(value = "0.0", inclusive = false, message = "price must be > 0")
    private BigDecimal price;

    private String imageUrl;

    @Min(value = 0, message = "stockQuantity must be >= 0")
    private Integer stockQuantity;

    private String categoryName;
}
