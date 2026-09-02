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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Category existingCategory() {
        Category category = new Category();
        category.setId(5L);
        category.setName("Electronics");
        category.setState(ProductState.ACTIVE);
        return category;
    }

    private Product sampleProduct() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Laptop");
        product.setDescription("A fast laptop");
        product.setPrice(new BigDecimal("999.99"));
        product.setStockQuantity(10);
        product.setCategory(existingCategory());
        product.setState(ProductState.ACTIVE);
        return product;
    }

    private ProductRequestDto validRequest() {
        ProductRequestDto request = new ProductRequestDto();
        request.setName("Laptop");
        request.setDescription("A fast laptop");
        request.setPrice(new BigDecimal("999.99"));
        request.setImageUrl("http://img/laptop.png");
        request.setStockQuantity(10);
        request.setCategoryName("Electronics");
        return request;
    }

    // ---------- getAllProducts ----------

    @Test
    void getAllProducts_returnsAllFromRepository() {
        List<Product> products = List.of(sampleProduct());
        when(productRepository.findAll()).thenReturn(products);

        List<Product> result = productService.getAllProducts();

        assertThat(result).hasSize(1);
        verify(productRepository).findAll();
    }

    // ---------- getProductById ----------

    @Test
    void getProductById_whenExists_returnsProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct()));

        Product result = productService.getProductById(1L);

        assertThat(result.getName()).isEqualTo("Laptop");
    }

    @Test
    void getProductById_whenMissing_throwsNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ---------- createProduct ----------

    @Test
    void createProduct_withExistingCategory_reusesCategory() {
        Category category = existingCategory();
        when(categoryRepository.findByName("Electronics")).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product result = productService.createProduct(validRequest());

        assertThat(result.getCategory()).isSameAs(category);
        assertThat(result.getState()).isEqualTo(ProductState.ACTIVE);
        assertThat(result.getName()).isEqualTo("Laptop");
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void createProduct_withNewCategory_createsCategory() {
        when(categoryRepository.findByName("Electronics")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product result = productService.createProduct(validRequest());

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(categoryCaptor.capture());
        assertThat(categoryCaptor.getValue().getName()).isEqualTo("Electronics");
        assertThat(categoryCaptor.getValue().getState()).isEqualTo(ProductState.ACTIVE);
        assertThat(result.getCategory().getName()).isEqualTo("Electronics");
    }

    @Test
    void createProduct_withNullRequest_throwsIllegalArgument() {
        assertThatThrownBy(() -> productService.createProduct(null))
                .isInstanceOf(IllegalArgumentException.class);

        verify(productRepository, never()).save(any());
    }

    // ---------- updateProduct ----------

    @Test
    void updateProduct_whenExists_appliesPartialChanges() {
        Product existing = sampleProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductUpdateDto update = new ProductUpdateDto();
        update.setName("Laptop Pro");
        update.setPrice(new BigDecimal("1299.99"));

        Product result = productService.updateProduct(1L, update);

        assertThat(result.getName()).isEqualTo("Laptop Pro");
        assertThat(result.getPrice()).isEqualByComparingTo("1299.99");
        // untouched fields remain
        assertThat(result.getStockQuantity()).isEqualTo(10);
        assertThat(result.getDescription()).isEqualTo("A fast laptop");
    }

    @Test
    void updateProduct_withNewCategoryName_createsCategory() {
        Product existing = sampleProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findByName("Books")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductUpdateDto update = new ProductUpdateDto();
        update.setCategoryName("Books");

        Product result = productService.updateProduct(1L, update);

        assertThat(result.getCategory().getName()).isEqualTo("Books");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void updateProduct_whenMissing_throwsNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(99L, new ProductUpdateDto()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateProduct_whenOptimisticLockFails_throwsConflict() {
        Product existing = sampleProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Product.class, 1L));

        ProductUpdateDto update = new ProductUpdateDto();
        update.setName("Laptop Pro");

        assertThatThrownBy(() -> productService.updateProduct(1L, update))
                .isInstanceOf(ConflictException.class);
    }

    // ---------- deleteProduct ----------

    @Test
    void deleteProduct_whenExists_softDeletesToInactive() {
        Product existing = sampleProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        productService.deleteProduct(1L);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getState()).isEqualTo(ProductState.INACTIVE);
    }

    @Test
    void deleteProduct_whenMissing_throwsNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository, never()).save(any());
    }

    // ---------- searchProducts ----------

    @Test
    void searchProducts_withAllFilters_delegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Product> page = new PageImpl<>(List.of(sampleProduct()), pageable, 1);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<Product> result = productService.searchProducts(
                "Laptop", "Electronics", ProductState.ACTIVE,
                new BigDecimal("100"), new BigDecimal("2000"), pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(productRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void searchProducts_withNoFilters_delegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Product> page = new PageImpl<>(List.of(sampleProduct()), pageable, 1);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<Product> result = productService.searchProducts(
                null, null, null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(productRepository).findAll(any(Specification.class), any(Pageable.class));
    }
}
