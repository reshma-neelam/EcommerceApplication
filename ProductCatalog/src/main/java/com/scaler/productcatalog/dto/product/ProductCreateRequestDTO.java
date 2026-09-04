package com.scaler.productcatalog.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCreateRequestDTO {

    @NotBlank
    @Size(max = 64)
    private String sku;

    @NotBlank
    @Size(max = 255)
    private String name;

    private String description;

    @Size(max = 128)
    private String brand;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "basePrice must be > 0")
    private BigDecimal basePrice;

    @NotBlank
    @Pattern(regexp = "[A-Za-z]{3}", message = "currency must be a 3-letter ISO-4217 code")
    private String currency;

    @NotEmpty(message = "at least one categoryId is required")
    private List<UUID> categoryIds;

    @NotNull(message = "primaryCategoryId is required")
    private UUID primaryCategoryId;

    private Integer initialOnHandQuantity;
}
