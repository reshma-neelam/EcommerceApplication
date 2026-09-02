package com.scaler.productcatalog.service;

import com.scaler.productcatalog.dto.ProductRequestDto;
import com.scaler.productcatalog.dto.ProductUpdateDto;
import com.scaler.productcatalog.enums.ProductState;
import com.scaler.productcatalog.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {
    List<Product> getAllProducts();
    Product getProductById(Long id);
    Product createProduct(ProductRequestDto request);
    Product updateProduct(Long id, ProductUpdateDto request);
    void deleteProduct(Long id);
    Page<Product> searchProducts(String query, String category, ProductState state,
                                 BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
}