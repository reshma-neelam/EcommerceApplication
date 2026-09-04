package com.scaler.productcatalog.dto.product;

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
public class ImageResponseDTO {
    private UUID id;
    private UUID productId;
    private String url;
    private String altText;
    private int displayOrder;
    private boolean primary;
    private Instant createdAt;
}
