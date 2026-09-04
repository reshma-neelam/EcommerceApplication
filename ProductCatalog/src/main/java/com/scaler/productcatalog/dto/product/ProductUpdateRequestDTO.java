package com.scaler.productcatalog.dto.product;

import com.scaler.productcatalog.enums.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Partial update. Null fields are left unchanged; provided string fields must be non-blank.
 * {@code expectedVersion} enforces optimistic concurrency.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductUpdateRequestDTO {

    @Pattern(regexp = "\\S.*", message = "name must not be blank")
    @Size(max = 255)
    private String name;

    private String description;

    @Size(max = 128)
    private String brand;

    @DecimalMin(value = "0.0", inclusive = false, message = "basePrice must be > 0")
    private BigDecimal basePrice;

    @Pattern(regexp = "[A-Za-z]{3}", message = "currency must be a 3-letter ISO-4217 code")
    private String currency;

    private ProductStatus status;

    private List<UUID> categoryIds;

    private UUID primaryCategoryId;

    @NotNull(message = "expectedVersion is required")
    private Long expectedVersion;
}
