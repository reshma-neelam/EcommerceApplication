package com.scaler.productcatalog.repository;

import com.scaler.productcatalog.model.Product;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository
        extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsBySku(String sku);
}