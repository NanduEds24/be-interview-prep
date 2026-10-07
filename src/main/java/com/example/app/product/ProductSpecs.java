package com.example.app.product;

import com.example.app.product.ProductDtos.ProductFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Turns the optional filters into one WHERE clause: each filter that is present adds an AND condition. */
final class ProductSpecs {

    private ProductSpecs() {}

    static Specification<Product> from(ProductFilter filter) {
        List<Specification<Product>> specs = new ArrayList<>();
        // Exact match (case-sensitive) so the database can use idx_product_category; lower(category) couldn't.
        if (filter.category() != null && !filter.category().isBlank()) {
            String category = filter.category().trim();
            specs.add((root, query, cb) -> cb.equal(root.get("category"), category));
        }
        if (filter.minPrice() != null) {
            specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), filter.minPrice()));
        }
        if (filter.maxPrice() != null) {
            specs.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), filter.maxPrice()));
        }
        if (Boolean.TRUE.equals(filter.inStock())) {
            specs.add((root, query, cb) -> cb.greaterThan(root.get("stock"), 0));
        }
        if (filter.q() != null && !filter.q().isBlank()) {
            // Escape LIKE wildcards so "%" or "_" in the search text match literally.
            String text = filter.q().trim().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            String pattern = "%" + text + "%";
            specs.add((root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, '\\'));
        }
        return Specification.allOf(specs);
    }
}
