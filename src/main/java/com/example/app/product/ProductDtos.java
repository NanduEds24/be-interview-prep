package com.example.app.product;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;

public final class ProductDtos {

    private ProductDtos() {}

    public record ProductRequest(
            @NotBlank(message = "name is required") @Size(max = 100, message = "name must be at most 100 characters") String name,
            @NotBlank(message = "category is required") @Size(max = 50, message = "category must be at most 50 characters") String category,
            @NotNull(message = "price is required") @DecimalMin(value = "0.01", message = "price must be at least 0.01")
            @Digits(integer = 8, fraction = 2, message = "price must have at most 2 decimals") BigDecimal price,
            @NotNull(message = "stock is required") @Min(value = 0, message = "stock cannot be negative") Integer stock,
            @NotNull(message = "rating is required")
            @DecimalMin(value = "0.0", message = "rating must be between 0 and 5")
            @DecimalMax(value = "5.0", message = "rating must be between 0 and 5") Double rating) {}

    // Immutable record: safe to keep in the cache and hand to many requests.
    public record ProductResponse(
            Long id, String name, String category, BigDecimal price, int stock, double rating, Instant createdAt) {

        static ProductResponse from(Product p) {
            return new ProductResponse(p.getId(), p.getName(), p.getCategory(), p.getPrice(), p.getStock(),
                    p.getRating(), p.getCreatedAt());
        }
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

        static <T> PageResponse<T> from(Page<T> page) {
            return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
                    page.getTotalPages());
        }
    }

    /** All filters are optional; null means "don't filter on this". */
    public record ProductFilter(String category, BigDecimal minPrice, BigDecimal maxPrice, Boolean inStock, String q) {}
}
