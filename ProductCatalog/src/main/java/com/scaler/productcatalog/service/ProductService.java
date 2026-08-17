package com.scaler.productcatalog.service;

import com.scaler.productcatalog.dto.ProductRequestDto;
import com.scaler.productcatalog.model.Product;

import java.util.List;

public interface ProductService {
    List<Product> getAllProducts();
    Product getProductById(Long id);
    Product createProduct(ProductRequestDto request);
}