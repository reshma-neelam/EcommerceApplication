package com.scaler.productcatalog.repository;

import com.scaler.productcatalog.enums.CategoryStatus;
import com.scaler.productcatalog.model.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Category> findByStatus(CategoryStatus status);
}
