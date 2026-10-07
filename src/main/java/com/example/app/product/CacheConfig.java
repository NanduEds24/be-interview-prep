package com.example.app.product;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/** Turns on @Cacheable/@CacheEvict. With no cache library added, Spring Boot uses an in-memory ConcurrentMap. */
@Configuration
@EnableCaching
public class CacheConfig {}
