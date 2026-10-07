package com.example.app.order;

import com.example.app.order.OrderDtos.ItemRequest;
import com.example.app.order.OrderDtos.OrderResponse;
import com.example.app.order.OrderDtos.PlaceOrderRequest;
import com.example.app.order.OrderDtos.PlaceOrderResult;
import com.example.app.product.StockService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * Places and cancels orders. Two guarantees:
 * - No overselling: StockService decrements with one conditional UPDATE per product, inside one
 *   transaction per order, so either every item is reserved or the whole order rolls back.
 * - No duplicate orders on retry: the client sends an Idempotency-Key header; (customer, key) is unique.
 */
@Service
public class OrderService {

    static final int MAX_KEY_LENGTH = 64;
    static final int MAX_QUANTITY_PER_PRODUCT = 1000;

    private final OrderRepository repository;
    private final StockService stockService;
    private final TransactionTemplate tx;
    private final TransactionTemplate readOnlyTx;

    public OrderService(OrderRepository repository, StockService stockService, PlatformTransactionManager txManager) {
        this.repository = repository;
        this.stockService = stockService;
        this.tx = new TransactionTemplate(txManager);
        this.readOnlyTx = new TransactionTemplate(txManager);
        this.readOnlyTx.setReadOnly(true);
    }

    public PlaceOrderResult place(String customerEmail, String idempotencyKey, PlaceOrderRequest request) {
        String key = validateKey(idempotencyKey);
        // Merge repeated products and sort by id: every transaction locks product rows in the same order,
        // so two orders for products {1, 2} and {2, 1} can't deadlock each other.
        SortedMap<Long, Integer> items = new TreeMap<>();
        for (ItemRequest item : request.items()) {
            items.merge(item.productId(), item.quantity(), Integer::sum);
        }
        items.forEach((productId, quantity) -> {
            if (quantity > MAX_QUANTITY_PER_PRODUCT) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Total quantity for product " + productId
                        + " must be at most " + MAX_QUANTITY_PER_PRODUCT);
            }
        });
        String fingerprint = sha256(items.toString());

        // Fast path for the usual retry (the first request already finished): no locks, no stock work.
        Optional<PlaceOrderResult> replay = findExisting(customerEmail, key, fingerprint);
        if (replay.isPresent()) {
            return replay.get();
        }
        try {
            return tx.execute(status -> create(customerEmail, key, fingerprint, items));
        } catch (DataIntegrityViolationException | PessimisticLockingFailureException e) {
            // Another request with the same key got the unique index first. Our transaction (and its stock
            // reservations) rolled back. If that request committed, return its order; if it is still running,
            // tell the client to retry shortly instead of guessing.
            return findExisting(customerEmail, key, fingerprint).orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.CONFLICT, "A request with Idempotency-Key " + key + " is still being processed; retry shortly"));
        }
    }

    public OrderResponse get(String customerEmail, Long id) {
        return readOnlyTx.execute(status -> OrderResponse.from(find(customerEmail, id)));
    }

    /** Cancels and returns the stock, all in one transaction. */
    public OrderResponse cancel(String customerEmail, Long id) {
        return tx.execute(status -> {
            List<OrderItem> items = List.copyOf(find(customerEmail, id).getItems());
            if (repository.markCancelled(id) == 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Order " + id + " is already cancelled");
            }
            items.forEach(item -> stockService.release(item.getProductId(), item.getQuantity()));
            return OrderResponse.from(find(customerEmail, id));
        });
    }

    private PlaceOrderResult create(String customerEmail, String key, String fingerprint, SortedMap<Long, Integer> items) {
        CustomerOrder order = new CustomerOrder(customerEmail, key, fingerprint);
        items.forEach(order::addItem);
        // Insert first: this claims the idempotency key, so a concurrent duplicate fails fast on the unique constraint.
        repository.saveAndFlush(order);
        for (Map.Entry<Long, Integer> item : items.entrySet()) {
            stockService.reserve(item.getKey(), item.getValue()); // throws 404/409 -> whole order rolls back
        }
        return new PlaceOrderResult(OrderResponse.from(order), true);
    }

    private Optional<PlaceOrderResult> findExisting(String customerEmail, String key, String fingerprint) {
        return readOnlyTx.execute(status -> repository.findByCustomerEmailAndIdempotencyKey(customerEmail, key)
                .map(order -> {
                    if (!order.getRequestFingerprint().equals(fingerprint)) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "Idempotency-Key " + key + " was already used for a different order");
                    }
                    return new PlaceOrderResult(OrderResponse.from(order), false);
                }));
    }

    /** Orders of other customers are reported as not found, so ids can't be probed. */
    private CustomerOrder find(String customerEmail, Long id) {
        return repository.findByIdAndCustomerEmail(id, customerEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order " + id + " not found"));
    }

    /** The key is used exactly as sent (no trimming), so two different keys never collide. */
    private static String validateKey(String key) {
        if (key == null || key.isBlank() || key.length() > MAX_KEY_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Idempotency-Key header must be 1 to " + MAX_KEY_LENGTH + " characters");
        }
        return key;
    }

    // Fixed 64-character fingerprint, however many items the order has.
    private static String sha256(String text) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
