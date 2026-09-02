package com.scaler.productcatalog.repository;

import com.scaler.productcatalog.enums.ProductState;
import com.scaler.productcatalog.model.Category;
import com.scaler.productcatalog.model.Product;
import com.scaler.productcatalog.specification.ProductSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category electronics;

    @BeforeEach
    void setUp() {
        electronics = new Category();
        electronics.setName("Electronics");
        electronics.setState(ProductState.ACTIVE);
        electronics = categoryRepository.save(electronics);

        Category books = new Category();
        books.setName("Books");
        books.setState(ProductState.ACTIVE);
        books = categoryRepository.save(books);

        productRepository.save(product("Laptop", "A fast gaming laptop",
                new BigDecimal("999.99"), electronics, ProductState.ACTIVE));
        productRepository.save(product("Phone", "A smart phone",
                new BigDecimal("499.99"), electronics, ProductState.ACTIVE));
        productRepository.save(product("Old Tablet", "Discontinued device",
                new BigDecimal("199.99"), electronics, ProductState.INACTIVE));
        productRepository.save(product("Novel", "A gripping laptop-free story",
                new BigDecimal("19.99"), books, ProductState.ACTIVE));
    }

    private Product product(String name, String description, BigDecimal price,
                            Category category, ProductState state) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setStockQuantity(5);
        product.setCategory(category);
        product.setState(state);
        return product;
    }

    @Test
    void saveAndFindById_persistsProductWithAuditFields() {
        Product product = product("Mouse", "Wireless mouse",
                new BigDecimal("29.99"), electronics, ProductState.ACTIVE);

        Product saved = productRepository.save(product);

        Optional<Product> found = productRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Mouse");
        assertThat(found.get().getCreatedAt()).isNotNull();
        assertThat(found.get().getUpdatedAt()).isNotNull();
        assertThat(found.get().getVersion()).isNotNull();
    }

    @Test
    void searchByNameOrDescription_matchesCaseInsensitively() {
        Specification<Product> spec = ProductSpecification.nameOrDescriptionContains("laptop");

        Page<Product> result = productRepository.findAll(spec, PageRequest.of(0, 10));

        // "Laptop" (name) and "Novel" (description contains "laptop-free")
        assertThat(result.getContent()).extracting(Product::getName)
                .containsExactlyInAnyOrder("Laptop", "Novel");
    }

    @Test
    void searchByCategory_returnsOnlyMatchingCategory() {
        Specification<Product> spec = ProductSpecification.hasCategory("Books");

        Page<Product> result = productRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(Product::getName)
                .containsExactly("Novel");
    }

    @Test
    void searchByState_filtersByProductState() {
        Specification<Product> spec = ProductSpecification.hasState(ProductState.INACTIVE);

        Page<Product> result = productRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(Product::getName)
                .containsExactly("Old Tablet");
    }

    @Test
    void searchByPriceRange_appliesBothBounds() {
        Specification<Product> spec = Specification
                .where(ProductSpecification.priceGreaterThanOrEqual(new BigDecimal("200")))
                .and(ProductSpecification.priceLessThanOrEqual(new BigDecimal("600")));

        Page<Product> result = productRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(Product::getName)
                .containsExactly("Phone");
    }

    @Test
    void searchWithCombinedSpecs_appliesAllConditions() {
        Specification<Product> spec = Specification
                .where(ProductSpecification.hasCategory("Electronics"))
                .and(ProductSpecification.hasState(ProductState.ACTIVE))
                .and(ProductSpecification.priceGreaterThanOrEqual(new BigDecimal("500")));

        Page<Product> result = productRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(Product::getName)
                .containsExactly("Laptop");
    }

    @Test
    void pagination_limitsResults() {
        Pageable firstPage = PageRequest.of(0, 2);

        Page<Product> result = productRepository.findAll(firstPage);

        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }
}
