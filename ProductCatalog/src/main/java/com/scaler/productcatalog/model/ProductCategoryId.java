package com.scaler.productcatalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class ProductCategoryId implements Serializable {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "product_id", columnDefinition = "char(36)")
    private UUID productId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "category_id", columnDefinition = "char(36)")
    private UUID categoryId;

    public ProductCategoryId(UUID productId, UUID categoryId) {
        this.productId = productId;
        this.categoryId = categoryId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProductCategoryId that)) {
            return false;
        }
        return Objects.equals(productId, that.productId) && Objects.equals(categoryId, that.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, categoryId);
    }
}
