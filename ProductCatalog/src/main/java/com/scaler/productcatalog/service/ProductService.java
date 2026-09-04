package com.scaler.productcatalog.service;

import com.scaler.productcatalog.dto.product.ImageCreateRequestDTO;
import com.scaler.productcatalog.dto.product.ImageResponseDTO;
import com.scaler.productcatalog.dto.product.ImageUpdateRequestDTO;
import com.scaler.productcatalog.dto.product.ProductCreateRequestDTO;
import com.scaler.productcatalog.dto.product.ProductResponseDTO;
import com.scaler.productcatalog.dto.product.ProductUpdateRequestDTO;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    ProductResponseDTO create(ProductCreateRequestDTO request);

    ProductResponseDTO update(UUID id, ProductUpdateRequestDTO request);

    void softDelete(UUID id);

    ProductResponseDTO getActive(UUID id);

    Page<ProductResponseDTO> searchActive(String query, UUID categoryId, BigDecimal minPrice,
                                          BigDecimal maxPrice, Pageable pageable);

    ImageResponseDTO addImage(UUID productId, ImageCreateRequestDTO request);

    ImageResponseDTO updateImage(UUID productId, UUID imageId, ImageUpdateRequestDTO request);

    void deleteImage(UUID productId, UUID imageId);
}
