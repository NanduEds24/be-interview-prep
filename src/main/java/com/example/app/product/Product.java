package com.example.app.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;

// Indexes on the filter columns keep the list query fast as the catalog grows.
@Entity
@Table(indexes = {
        @Index(name = "idx_product_category", columnList = "category"),
        @Index(name = "idx_product_price", columnList = "price")
})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false)
    private double rating;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Optimistic lock: an admin update that read the product before an order changed its stock fails
     * (409) instead of silently writing the old stock back. The stock UPDATE queries bump it too.
     */
    @Version
    private long version;

    protected Product() {}

    public Product(String name, String category, BigDecimal price, int stock, double rating, Instant createdAt) {
        update(name, category, price, stock, rating);
        this.createdAt = createdAt;
    }

    public void update(String name, String category, BigDecimal price, int stock, double rating) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.rating = rating;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public BigDecimal getPrice() { return price; }
    public int getStock() { return stock; }
    public double getRating() { return rating; }
    public Instant getCreatedAt() { return createdAt; }
}
