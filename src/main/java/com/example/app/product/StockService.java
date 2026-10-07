package com.example.app.product;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

/**
 * Reserves and releases stock for orders. MANDATORY propagation: these must run inside the caller's
 * transaction, so one failing item rolls back every earlier reservation of the same order.
 */
@Service
public class StockService {

    private final ProductRepository repository;
    private final Cache cache;

    public StockService(ProductRepository repository, CacheManager cacheManager) {
        this.repository = repository;
        this.cache = cacheManager.getCache(ProductService.CACHE);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void reserve(Long productId, int quantity) {
        if (repository.decrementStock(productId, quantity) == 0) {
            Product product = repository.findById(productId).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Product " + productId + " not found"));
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock for product " + productId
                    + " (" + product.getName() + "): requested " + quantity + ", available " + product.getStock());
        }
        evictAfterCommit(productId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void release(Long productId, int quantity) {
        repository.incrementStock(productId, quantity);
        evictAfterCommit(productId);
    }

    // The cached product now shows the wrong stock; drop it once the change is committed (not before,
    // or a concurrent read could re-cache the old value).
    private void evictAfterCommit(Long productId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cache.evict(productId);
            }
        });
    }
}
