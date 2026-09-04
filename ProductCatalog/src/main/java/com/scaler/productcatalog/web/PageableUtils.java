package com.scaler.productcatalog.web;

import com.scaler.productcatalog.exception.BadRequestException;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Bounds page size and restricts sorting to an allow-list of fields. */
public final class PageableUtils {

    public static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_PRODUCT_SORTS = Set.of("name", "basePrice", "createdAt", "updatedAt");

    private PageableUtils() {
    }

    public static Pageable sanitizeProductPageable(Pageable pageable) {
        int size = pageable.getPageSize();
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("INVALID_PAGE_SIZE",
                    "page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        Sort sort = pageable.getSort();
        for (Sort.Order order : sort) {
            if (!ALLOWED_PRODUCT_SORTS.contains(order.getProperty())) {
                throw new BadRequestException("INVALID_SORT",
                        "sort field not allowed: " + order.getProperty());
            }
        }
        Sort effectiveSort = sort.isSorted() ? sort : Sort.by(Sort.Direction.DESC, "createdAt");
        return PageRequest.of(pageable.getPageNumber(), size, effectiveSort);
    }
}
