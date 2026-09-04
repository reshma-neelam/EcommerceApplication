package com.scaler.productcatalog.repository;

import com.scaler.productcatalog.model.ProductInventory;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductInventoryRepository extends JpaRepository<ProductInventory, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ProductInventory i where i.productId = :productId")
    Optional<ProductInventory> findByProductIdForUpdate(@Param("productId") UUID productId);
}
