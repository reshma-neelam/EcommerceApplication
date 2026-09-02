package com.scaler.productcatalog.specification;

import com.scaler.productcatalog.enums.ProductState;
import com.scaler.productcatalog.model.Product;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public final class ProductSpecification {

    private ProductSpecification() {}

    public static Specification<Product> nameOrDescriptionContains(String query) {
        String pattern = "%" + query.toLowerCase() + "%";
        return (root, cq, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("description")), pattern)
        );
    }

    public static Specification<Product> hasCategory(String categoryName) {
        return (root, cq, cb) ->
                cb.equal(root.join("category").get("name"), categoryName);
    }

    public static Specification<Product> hasState(ProductState state) {
        return (root, cq, cb) -> cb.equal(root.get("state"), state);
    }

    public static Specification<Product> priceGreaterThanOrEqual(BigDecimal min) {
        return (root, cq, cb) -> cb.greaterThanOrEqualTo(root.get("price"), min);
    }

    public static Specification<Product> priceLessThanOrEqual(BigDecimal max) {
        return (root, cq, cb) -> cb.lessThanOrEqualTo(root.get("price"), max);
    }
}
