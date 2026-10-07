package com.example.app.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.app.order.OrderDtos.ItemRequest;
import com.example.app.order.OrderDtos.PlaceOrderRequest;
import com.example.app.order.OrderDtos.PlaceOrderResult;
import com.example.app.product.Product;
import com.example.app.product.ProductRepository;
import com.example.app.product.ProductService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Not @Transactional: every order must commit in its own transaction, exactly like concurrent HTTP
 * requests, otherwise the database row locks and rollbacks under test would never really happen.
 */
@SpringBootTest
class OrderConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    private final List<Long> createdProducts = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        orderRepository.deleteAll();
        productRepository.deleteAllById(createdProducts);
    }

    private long product(int stock) {
        long id = productRepository
                .save(new Product("Hot Item", "Toys", new BigDecimal("1.00"), stock, 5.0, Instant.now()))
                .getId();
        createdProducts.add(id);
        return id;
    }

    private int stock(long productId) {
        return productRepository.findById(productId).orElseThrow().getStock();
    }

    private static PlaceOrderRequest request(long productId, int quantity) {
        return new PlaceOrderRequest(List.of(new ItemRequest(productId, quantity)));
    }

    /** Starts all tasks at the same instant and returns each result or the exception it threw. */
    private static <T> List<Object> runAtOnce(int count, Callable<T> task) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(count);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                return task.call();
            }));
        }
        start.countDown();
        List<Object> results = new ArrayList<>();
        for (Future<T> future : futures) {
            try {
                results.add(future.get());
            } catch (ExecutionException e) {
                results.add(e.getCause());
            }
        }
        pool.shutdown();
        return results;
    }

    @Test
    void fiftySimultaneousOrdersForStockTenSellExactlyTen() throws Exception {
        long productId = product(10);
        var keys = new java.util.concurrent.atomic.AtomicInteger();

        List<Object> results = runAtOnce(
                50, () -> orderService.place("buyer@test.com", "key-" + keys.incrementAndGet(), request(productId, 1)));

        long succeeded =
                results.stream().filter(r -> r instanceof PlaceOrderResult).count();
        long conflicts = results.stream()
                .filter(r -> r instanceof ResponseStatusException e
                        && e.getStatusCode().value() == 409)
                .count();
        assertThat(succeeded).isEqualTo(10);
        assertThat(conflicts).isEqualTo(40);
        assertThat(stock(productId)).isZero();
        assertThat(orderRepository.count()).isEqualTo(10);
    }

    @Test
    void simultaneousRetriesWithSameKeyCreateOneOrder() throws Exception {
        long productId = product(10);

        List<Object> results =
                runAtOnce(20, () -> orderService.place("buyer@test.com", "same-key", request(productId, 1)));

        assertThat(results).allMatch(r -> r instanceof PlaceOrderResult);
        assertThat(results.stream()
                        .map(r -> ((PlaceOrderResult) r).order().id())
                        .distinct())
                .hasSize(1);
        assertThat(results.stream().filter(r -> ((PlaceOrderResult) r).created()))
                .hasSize(1);
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(stock(productId)).isEqualTo(9);
    }

    @Test
    void orderIsAllOrNothing() {
        long plenty = product(5);
        long scarce = product(1);
        PlaceOrderRequest request =
                new PlaceOrderRequest(List.of(new ItemRequest(plenty, 2), new ItemRequest(scarce, 2)));

        assertThatThrownBy(() -> orderService.place("buyer@test.com", "mixed", request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Insufficient stock for product " + scarce);
        assertThat(stock(plenty)).isEqualTo(5);
        assertThat(stock(scarce)).isEqualTo(1);
        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void staleProductUpdateCannotOverwriteReservedStock() {
        long productId = product(10);
        Product adminCopy = productRepository.findById(productId).orElseThrow(); // admin reads stock 10

        orderService.place("buyer@test.com", "before-admin", request(productId, 4)); // stock 6, version bumped

        adminCopy.update("Renamed", "Toys", new BigDecimal("1.00"), 10, 5.0);
        assertThatThrownBy(() -> productRepository.save(adminCopy))
                .isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(stock(productId)).isEqualTo(6);
    }

    @Test
    void stockChangesEvictTheCachedProduct() {
        long productId = product(5);
        assertThat(productService.get(productId).stock()).isEqualTo(5); // now cached

        long orderId = orderService
                .place("buyer@test.com", "cache-key", request(productId, 2))
                .order()
                .id();
        assertThat(productService.get(productId).stock()).isEqualTo(3);

        orderService.cancel("buyer@test.com", orderId);
        assertThat(productService.get(productId).stock()).isEqualTo(5);
    }
}
