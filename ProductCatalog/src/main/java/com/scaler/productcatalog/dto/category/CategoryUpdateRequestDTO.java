package com.scaler.productcatalog.dto.category;

import com.scaler.productcatalog.enums.CategoryStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public class CategoryUpdateRequestDTO {

    @Pattern(regexp = "\\S.*", message = "name must not be blank")
    @Size(max = 128)
    private String name;

    @Size(max = 160)
    @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*", message = "slug must be lowercase words separated by hyphens")
    private String slug;

    private UUID parentCategoryId;

    private CategoryStatus status;
}
