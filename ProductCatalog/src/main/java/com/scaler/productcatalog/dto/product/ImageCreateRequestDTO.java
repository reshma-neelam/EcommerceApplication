package com.scaler.productcatalog.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public class ImageCreateRequestDTO {

    @NotBlank
    @Size(max = 1024)
    @Pattern(regexp = "^https?://.+", message = "url must be an absolute http(s) URL")
    private String url;

    @Size(max = 255)
    private String altText;

    private int displayOrder;

    private boolean primary;
}
