package com.example.app.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.app.product.Product;
import com.example.app.product.ProductRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** HTTP behaviour. Concurrency and all-or-nothing rollback are in OrderConcurrencyTest (needs real commits). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser("alice@test.com")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    private long productId;

    @BeforeEach
    void createProduct() {
        productId = productRepository.save(new Product("Order Lamp", "Home", new BigDecimal("10.00"), 5, 4.0,
                Instant.now())).getId();
    }

    private ResultActions order(String key, long product, int quantity) throws Exception {
        String body = "{\"items\": [{\"productId\": %d, \"quantity\": %d}]}".formatted(product, quantity);
        return mockMvc.perform(post("/api/orders").header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private int stock() {
        return productRepository.findById(productId).orElseThrow().getStock();
    }

    @Test
    void placesOrderAndReservesStock() throws Exception {
        order("key-1", productId, 2)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.items[0].productId").value(productId))
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        assertThat(stock()).isEqualTo(3);
    }

    @Test
    void retryWithSameKeyReturnsSameOrderOnce() throws Exception {
        String first = order("retry-key", productId, 2).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String second = order("retry-key", productId, 2).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat((Integer) JsonPath.read(second, "$.id")).isEqualTo(JsonPath.read(first, "$.id"));
        assertThat((String) JsonPath.read(second, "$.createdAt")).isEqualTo(JsonPath.read(first, "$.createdAt"));
        assertThat(stock()).isEqualTo(3);
    }

    @Test
    void sameKeyWithDifferentItemsReturns409() throws Exception {
        order("reused-key", productId, 1).andExpect(status().isCreated());

        order("reused-key", productId, 2)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Idempotency-Key reused-key was already used for a different order"));
    }

    @Test
    void insufficientStockReturns409WithMessage() throws Exception {
        order("too-many", productId, 6)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(
                        "Insufficient stock for product " + productId + " (Order Lamp): requested 6, available 5"));
    }

    @Test
    void unknownProductReturns404() throws Exception {
        order("ghost", 999_999, 1)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Product 999999 not found"));
    }

    @Test
    void missingOrBlankKeyReturns400() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\": [{\"productId\": 1, \"quantity\": 1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        order(" ", productId, 1).andExpect(status().isBadRequest());
    }

    @Test
    void invalidItemsReturnFieldErrors() throws Exception {
        mockMvc.perform(post("/api/orders").header("Idempotency-Key", "bad").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.items").value("items must contain at least one item"));
        order("bad-qty", productId, 0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['items[0].quantity']").value("quantity must be at least 1"));
    }

    @Test
    void repeatedProductLinesCannotExceedTheQuantityLimit() throws Exception {
        String line = "{\"productId\": %d, \"quantity\": 1000}".formatted(productId);
        mockMvc.perform(post("/api/orders").header("Idempotency-Key", "split").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\": [" + line + "," + line + "]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Total quantity for product " + productId + " must be at most 1000"));
    }

    @Test
    void nonNumericOrderIdReturns400() throws Exception {
        mockMvc.perform(get("/api/orders/abc")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mockMvc.perform(post("/api/orders/abc/cancel")).andExpect(status().isBadRequest());
    }

    @Test
    void getsOwnOrder() throws Exception {
        String json = order("get-me", productId, 1).andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(json, "$.id");

        mockMvc.perform(get("/api/orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void unknownOrOtherCustomersOrderReturns404() throws Exception {
        mockMvc.perform(get("/api/orders/999999")).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/orders/999999/cancel")).andExpect(status().isNotFound());

        String json = order("alice-order", productId, 1).andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(json, "$.id");
        mockMvc.perform(get("/api/orders/{id}", id).with(user("bob@test.com"))).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/orders/{id}/cancel", id).with(user("bob@test.com"))).andExpect(status().isNotFound());
    }

    @Test
    void cancelReturnsStockAndCannotRepeat() throws Exception {
        String json = order("to-cancel", productId, 4).andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(json, "$.id");
        assertThat(stock()).isEqualTo(1);

        mockMvc.perform(post("/api/orders/{id}/cancel", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(stock()).isEqualTo(5);

        mockMvc.perform(post("/api/orders/{id}/cancel", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Order " + id + " is already cancelled"));
        assertThat(stock()).isEqualTo(5);
    }

    @Test
    @WithAnonymousUser
    void notLoggedInReturns401() throws Exception {
        order("anon", productId, 1).andExpect(status().isUnauthorized());
    }
}
