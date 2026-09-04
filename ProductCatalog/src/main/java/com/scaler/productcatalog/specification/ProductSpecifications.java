package com.scaler.productcatalog.specification;

import com.scaler.productcatalog.enums.ProductStatus;
import com.scaler.productcatalog.model.Product;
import com.scaler.productcatalog.model.ProductCategory;
import com.scaler.productcatalog.model.ProductCategoryId;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> notDeleted() {
        return (root, cq, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<Product> hasStatus(ProductStatus status) {
        return (root, cq, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Product> nameOrDescriptionContains(String query) {
        String pattern = "%" + query.toLowerCase() + "%";
        return (root, cq, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("description")), pattern));
    }

    public static Specification<Product> priceGreaterThanOrEqual(BigDecimal min) {
        return (root, cq, cb) -> cb.greaterThanOrEqualTo(root.get("basePrice"), min);
    }

    public static Specification<Product> priceLessThanOrEqual(BigDecimal max) {
        return (root, cq, cb) -> cb.lessThanOrEqualTo(root.get("basePrice"), max);
    }

    public static Specification<Product> inCategory(UUID categoryId) {
        return (root, cq, cb) -> {
            Subquery<UUID> sub = cq.subquery(UUID.class);
            Root<ProductCategory> pc = sub.from(ProductCategory.class);
            sub.select(pc.get("id").get("productId"));
            sub.where(cb.equal(pc.get("id").get("categoryId"), categoryId));
            return root.get("id").in(sub);
        };
    }
}
