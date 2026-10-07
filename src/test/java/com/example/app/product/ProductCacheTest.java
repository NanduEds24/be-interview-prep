package com.example.app.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.app.product.ProductDtos.ProductRequest;
import com.example.app.product.ProductDtos.ProductResponse;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Proves the cache works by counting repository calls with a Mockito spy. (Counting SQL would be misleading
 * here: inside one test transaction Hibernate's own first-level cache also skips repeat selects.)
 */
@SpringBootTest
@Transactional
class ProductCacheTest {

    @MockitoSpyBean
    private ProductRepository repository;

    @Autowired
    private ProductService service;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    @AfterEach
    void clearCache() {
        cacheManager.getCache(ProductService.CACHE).clear();
        clearInvocations(repository);
    }

    @Test
    void repeatedLookupsHitTheDatabaseOnce() {
        ProductResponse first = service.get(1L);
        ProductResponse second = service.get(1L);
        service.get(1L);

        assertThat(second).isEqualTo(first);
        verify(repository, times(1)).findById(1L);
    }

    @Test
    void updateEvictsSoNextLookupIsFresh() {
        service.get(1L);
        service.update(1L, new ProductRequest("Renamed", "Books", new BigDecimal("9.99"), 3, 4.0));

        assertThat(service.get(1L).name()).isEqualTo("Renamed");
        assertThat(service.get(1L).price()).isEqualByComparingTo("9.99");
    }

    @Test
    void deleteEvictsSoNextLookupIs404() {
        service.get(2L);
        service.delete(2L);

        assertThatThrownBy(() -> service.get(2L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Product 2 not found");
    }
}
