package com.scaler.productcatalog.repository;

import com.scaler.productcatalog.enums.ProductState;
import com.scaler.productcatalog.model.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    private Category saveCategory(String name) {
        Category category = new Category();
        category.setName(name);
        category.setState(ProductState.ACTIVE);
        return categoryRepository.save(category);
    }

    @Test
    void findByName_whenExists_returnsCategory() {
        saveCategory("Electronics");

        Optional<Category> found = categoryRepository.findByName("Electronics");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Electronics");
        assertThat(found.get().getId()).isNotNull();
    }

    @Test
    void findByName_whenMissing_returnsEmpty() {
        saveCategory("Electronics");

        Optional<Category> found = categoryRepository.findByName("Books");

        assertThat(found).isEmpty();
    }
}
