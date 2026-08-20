package com.scaler.productcatalog.service;

import com.scaler.productcatalog.dto.ProductRequestDto;
import com.scaler.productcatalog.enums.ProductState;
import com.scaler.productcatalog.model.Category;
import com.scaler.productcatalog.model.Product;
import com.scaler.productcatalog.repository.CategoryRepository;
import com.scaler.productcatalog.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    @Override
    @Transactional
    public Product createProduct(ProductRequestDto request) {
        // Bean Validation happens in controller using @Valid, but we keep minimal guards
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }

        // Find or create category by name
        Category category = categoryRepository.findByName(request.getCategoryName())
                .orElseGet(() -> {
                    Category c = new Category();
                    c.setName(request.getCategoryName());
                    c.setCatalogName(request.getCatalogName());
                    c.setState(ProductState.ACTIVE);
                    return categoryRepository.save(c);
                });

        // Build and save product
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(category);
        product.setCatalogName(request.getCatalogName());
        product.setState(ProductState.ACTIVE);

        return productRepository.save(product);
    }
}