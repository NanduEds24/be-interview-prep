package com.example.app.link;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;

@Entity
public class ShortLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 8)
    private String code;

    @Column(nullable = false, length = 2048)
    private String originalUrl;

    private Instant expiresAt;

    @Column(nullable = false)
    private long visitCount;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected ShortLink() {}

    public ShortLink(String code, String originalUrl, Instant expiresAt) {
        this.code = code;
        this.originalUrl = originalUrl;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public boolean isExpired() {
        return expiresAt != null && !expiresAt.isAfter(Instant.now());
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public long getVisitCount() {
        return visitCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
