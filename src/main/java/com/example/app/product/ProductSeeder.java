package com.example.app.product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Seeds 100 products on startup. A fixed random seed gives the same catalog every run. */
@Component
public class ProductSeeder implements ApplicationRunner {

    static final int COUNT = 100;
    private static final String[] CATEGORIES = {"Books", "Electronics", "Home", "Sports", "Toys"};
    private static final String[] ADJECTIVES = {"Classic", "Pro", "Smart", "Compact", "Deluxe"};
    private static final String[] NOUNS = {"Lamp", "Speaker", "Backpack", "Notebook", "Bottle", "Puzzle", "Headphones", "Chair"};

    private final ProductRepository repository;

    public ProductSeeder(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        Random random = new Random(42);
        Instant now = Instant.now();
        List<Product> products = new ArrayList<>();
        for (int i = 1; i <= COUNT; i++) {
            String name = ADJECTIVES[random.nextInt(ADJECTIVES.length)] + " " + NOUNS[random.nextInt(NOUNS.length)] + " " + i;
            BigDecimal price = BigDecimal.valueOf(5 + random.nextDouble() * 495).setScale(2, RoundingMode.HALF_UP);
            int stock = random.nextInt(5) == 0 ? 0 : random.nextInt(200);
            double rating = Math.round(random.nextDouble() * 50) / 10.0;
            products.add(new Product(name, CATEGORIES[i % CATEGORIES.length], price, stock, rating,
                    now.minus(COUNT - i, ChronoUnit.DAYS)));
        }
        repository.saveAll(products);
    }
}
