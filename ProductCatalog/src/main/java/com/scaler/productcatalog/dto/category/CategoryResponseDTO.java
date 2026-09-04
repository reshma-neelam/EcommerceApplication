package com.scaler.productcatalog.dto.category;

import com.scaler.productcatalog.enums.CategoryStatus;
import java.time.Instant;
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
public class CategoryResponseDTO {
    private UUID id;
    private UUID parentCategoryId;
    private String name;
    private String slug;
    private CategoryStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
