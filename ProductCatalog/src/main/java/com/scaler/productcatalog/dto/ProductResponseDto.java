package com.scaler.productcatalog.dto;

import com.scaler.productcatalog.enums.ProductState;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@Builder
public class ProductResponseDto {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private String categoryName;
    private int stockQuantity;
    private ProductState state;
    private Date createdAt;
    private Date updatedAt;
}