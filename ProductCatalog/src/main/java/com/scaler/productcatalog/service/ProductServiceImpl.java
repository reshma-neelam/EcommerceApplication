package com.scaler.productcatalog.service;

import com.scaler.productcatalog.dto.ProductRequestDto;
import com.scaler.productcatalog.dto.ProductUpdateDto;
import com.scaler.productcatalog.enums.ProductState;
import com.scaler.productcatalog.exception.ConflictException;
import com.scaler.productcatalog.exception.ResourceNotFoundException;
import com.scaler.productcatalog.model.Category;
import com.scaler.productcatalog.model.Product;
import com.scaler.productcatalog.repository.CategoryRepository;
import com.scaler.productcatalog.repository.ProductRepository;
import com.scaler.productcatalog.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    @Override
    @Transactional
    public Product createProduct(ProductRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }

        Category category = categoryRepository.findByName(request.getCategoryName())
                .orElseGet(() -> {
                    Category c = new Category();
                    c.setName(request.getCategoryName());
                    c.setState(ProductState.ACTIVE);
                    return categoryRepository.save(c);
                });

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(category);
        product.setState(ProductState.ACTIVE);

        return productRepository.save(product);
    }

    @Override
    @Transactional
    public Product updateProduct(Long id, ProductUpdateDto request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getImageUrl() != null) {
            product.setImageUrl(request.getImageUrl());
        }
        if (request.getStockQuantity() != null) {
            product.setStockQuantity(request.getStockQuantity());
        }
        if (request.getCategoryName() != null) {
            Category category = categoryRepository.findByName(request.getCategoryName())
                    .orElseGet(() -> {
                        Category c = new Category();
                        c.setName(request.getCategoryName());
                        c.setState(ProductState.ACTIVE);
                        return categoryRepository.save(c);
                    });
            product.setCategory(category);
        }

        try {
            return productRepository.save(product);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ConflictException("Product was modified concurrently. Please retry with the latest version.");
        }
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        product.setState(ProductState.INACTIVE);
        productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> searchProducts(String query, String category, ProductState state,
                                        BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        Specification<Product> spec = Specification.where(null);

        if (query != null && !query.isBlank()) {
            spec = spec.and(ProductSpecification.nameOrDescriptionContains(query));
        }
        if (category != null && !category.isBlank()) {
            spec = spec.and(ProductSpecification.hasCategory(category));
        }
        if (state != null) {
            spec = spec.and(ProductSpecification.hasState(state));
        }
        if (minPrice != null) {
            spec = spec.and(ProductSpecification.priceGreaterThanOrEqual(minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and(ProductSpecification.priceLessThanOrEqual(maxPrice));
        }

        return productRepository.findAll(spec, pageable);
    }
}