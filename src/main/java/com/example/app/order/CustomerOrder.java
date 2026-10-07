package com.example.app.order;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * "Order" is a reserved SQL word, hence CustomerOrder. The unique (customer, idempotency key) pair is
 * what makes a retried request return the first order instead of creating a second one, even when the
 * retry arrives while the first request is still running.
 */
@Entity
@Table(
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_order_customer_key",
                        columnNames = {"customerEmail", "idempotencyKey"}))
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String customerEmail;

    @Column(nullable = false, length = 64)
    private String idempotencyKey;

    /** SHA-256 of the merged items, to detect a key reused for a different request. */
    @Column(nullable = false, length = 64)
    private String requestFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrderItem> items = new ArrayList<>();

    protected CustomerOrder() {}

    public CustomerOrder(String customerEmail, String idempotencyKey, String requestFingerprint) {
        this.customerEmail = customerEmail;
        this.idempotencyKey = idempotencyKey;
        this.requestFingerprint = requestFingerprint;
        this.status = OrderStatus.PLACED;
        // Microseconds = what the TIMESTAMP column stores, so the 201 response and later reads show the same value.
        this.createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    public void addItem(Long productId, int quantity) {
        items.add(new OrderItem(this, productId, quantity));
    }

    public Long getId() {
        return id;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }
}
