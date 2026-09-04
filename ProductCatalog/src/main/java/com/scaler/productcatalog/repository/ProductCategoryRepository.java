package com.scaler.productcatalog.repository;

import com.scaler.productcatalog.model.ProductCategory;
import com.scaler.productcatalog.model.ProductCategoryId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, ProductCategoryId> {

    List<ProductCategory> findByIdProductId(UUID productId);

    List<ProductCategory> findByIdCategoryId(UUID categoryId);

    void deleteByIdProductId(UUID productId);
}
