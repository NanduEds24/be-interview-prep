package com.example.app.product;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Turns on @Cacheable/@CacheEvict. With no cache library added, Spring Boot uses an in-memory ConcurrentMap.
 * The cache proxy is ordered just outside the transaction proxy (which uses LOWEST_PRECEDENCE), so an
 * eviction runs after the transaction has committed, not before.
 */
@Configuration
@EnableCaching(order = Ordered.LOWEST_PRECEDENCE - 1)
public class CacheConfig {}
