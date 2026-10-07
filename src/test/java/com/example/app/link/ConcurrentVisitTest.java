package com.example.app.link;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// Not @Transactional: each visit must commit in its own transaction, like real concurrent requests.
@SpringBootTest
class ConcurrentVisitTest {

    @Autowired
    private ShortLinkService service;

    @Autowired
    private ShortLinkRepository repository;

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void concurrentVisitsAreAllCounted() throws Exception {
        String code = service.shorten(new ShortenRequest("https://example.com", null)).getCode();
        int visits = 100;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(20);
        try {
            List<Future<String>> results = new ArrayList<>();
            for (int i = 0; i < visits; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return service.resolve(code, true);
                }));
            }
            start.countDown();
            for (Future<String> result : results) {
                result.get();
            }
        } finally {
            pool.shutdownNow(); // no leaked threads even if a visit failed
        }

        assertThat(service.stats(code).visitCount()).isEqualTo(visits);
    }
}
