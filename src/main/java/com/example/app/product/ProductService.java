package com.example.app.product;

import com.example.app.product.ProductDtos.PageResponse;
import com.example.app.product.ProductDtos.ProductFilter;
import com.example.app.product.ProductDtos.ProductRequest;
import com.example.app.product.ProductDtos.ProductResponse;
import java.time.Instant;
import java.util.Set;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductService {

    public static final String CACHE = "products";
    static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> SORTABLE = Set.of("id", "name", "category", "price", "stock", "rating", "createdAt");

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> list(ProductFilter filter, int page, int size, String sort) {
        if (filter.minPrice() != null && filter.maxPrice() != null && filter.minPrice().compareTo(filter.maxPrice()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minPrice cannot be greater than maxPrice");
        }
        PageRequest pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), parseSort(sort));
        return PageResponse.from(repository.findAll(ProductSpecs.from(filter), pageable).map(ProductResponse::from));
    }

    /** Cached by id. The first call queries the database; later calls are served from memory until evicted. */
    @Cacheable(cacheNames = CACHE, key = "#id")
    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return ProductResponse.from(find(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest r) {
        Product product = new Product(r.name(), r.category(), r.price(), r.stock(), r.rating(), Instant.now());
        return ProductResponse.from(repository.save(product));
    }

    /** Evicts rather than updates the cache entry: the next read reloads fresh data from the database. */
    @CacheEvict(cacheNames = CACHE, key = "#id")
    @Transactional
    public ProductResponse update(Long id, ProductRequest r) {
        Product product = find(id);
        product.update(r.name(), r.category(), r.price(), r.stock(), r.rating());
        return ProductResponse.from(product);
    }

    @CacheEvict(cacheNames = CACHE, key = "#id")
    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
    }

    private Product find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product " + id + " not found"));
    }

    // "price,desc" -> ORDER BY price DESC, id ASC. The id tie-breaker keeps pages stable when values repeat.
    private static Sort parseSort(String sort) {
        String[] parts = sort.split(",", -1);
        if (parts.length > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sort must be 'field' or 'field,asc|desc'");
        }
        String field = parts[0].trim();
        if (!SORTABLE.contains(field)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot sort by '" + field + "'. Allowed: " + String.join(", ", SORTABLE.stream().sorted().toList()));
        }
        String direction = parts.length > 1 ? parts[1].trim().toLowerCase() : "asc";
        if (!direction.equals("asc") && !direction.equals("desc")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sort direction must be asc or desc");
        }
        Sort primary = Sort.by(Sort.Direction.fromString(direction), field);
        return field.equals("id") ? primary : primary.and(Sort.by("id"));
    }
}
