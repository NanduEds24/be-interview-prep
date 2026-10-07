package com.example.app.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProductControllerTest {

    private static final String VALID_PRODUCT = """
            {"name": "Test Lamp", "category": "Home", "price": 19.99, "stock": 5, "rating": 4.5}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CacheManager cacheManager;

    // The database rolls back after each test but the cache doesn't, so empty it to avoid leaking state.
    @AfterEach
    void clearCache() {
        cacheManager.getCache(ProductService.CACHE).clear();
    }

    @Test
    void listsSeededProductsWithPageInfo() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(20))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(ProductSeeder.COUNT))
                .andExpect(jsonPath("$.totalPages").value(5));
    }

    @Test
    void pageSizeIsCappedAt100() throws Exception {
        mockMvc.perform(get("/api/products").param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.content.length()").value(100));
    }

    @Test
    void combinesAllFiltersAndSorts() throws Exception {
        String json = mockMvc.perform(get("/api/products")
                        .param("category", "Books")
                        .param("minPrice", "20")
                        .param("maxPrice", "400")
                        .param("inStock", "true")
                        .param("q", "o")
                        .param("sort", "price,desc")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<Map<String, Object>> products = JsonPath.read(json, "$.content");
        assertThat(products).isNotEmpty();
        double previousPrice = Double.MAX_VALUE;
        for (Map<String, Object> p : products) {
            double price = ((Number) p.get("price")).doubleValue();
            assertThat(p.get("category")).isEqualTo("Books");
            assertThat(price).isBetween(20.0, 400.0).isLessThanOrEqualTo(previousPrice);
            assertThat((Integer) p.get("stock")).isPositive();
            assertThat(((String) p.get("name")).toLowerCase()).contains("o");
            previousPrice = price;
        }
        assertThat(((Number) JsonPath.read(json, "$.totalElements")).intValue()).isEqualTo(products.size());
    }

    @Test
    void unknownSortFieldReturns400() throws Exception {
        mockMvc.perform(get("/api/products").param("sort", "password"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void malformedSortReturns400() throws Exception {
        mockMvc.perform(get("/api/products").param("sort", ",")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/products").param("sort", "price,desc,name")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/products").param("sort", "price,sideways")).andExpect(status().isBadRequest());
    }

    @Test
    void searchTreatsWildcardsLiterally() throws Exception {
        mockMvc.perform(get("/api/products").param("q", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/products").param("q", "_"))
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/products").param("q", "\\"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void invalidPagingOrPriceRangeReturns400() throws Exception {
        mockMvc.perform(get("/api/products").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.page").value("page cannot be negative"));
        mockMvc.perform(get("/api/products").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.size").value("size must be at least 1"));
        mockMvc.perform(get("/api/products").param("minPrice", "50").param("maxPrice", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("minPrice cannot be greater than maxPrice"));
    }

    @Test
    void getsOneProduct() throws Exception {
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void unknownProductReturns404() throws Exception {
        mockMvc.perform(get("/api/products/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Product 9999 not found"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCreatesUpdatesAndDeletes() throws Exception {
        String json = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PRODUCT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Lamp"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long id = ((Number) JsonPath.read(json, "$.id")).longValue();

        mockMvc.perform(put("/api/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PRODUCT.replace("19.99", "29.99")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(29.99));

        mockMvc.perform(delete("/api/products/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/products/{id}", id)).andExpect(status().isNotFound());
        mockMvc.perform(put("/api/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PRODUCT))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidProductReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"name\": \"\", \"category\": \"Home\", \"price\": 0, \"stock\": -1, \"rating\": 6}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.price").exists())
                .andExpect(jsonPath("$.errors.stock").exists())
                .andExpect(jsonPath("$.errors.rating").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void missingStockAndRatingReturn400InsteadOfZero() throws Exception {
        String body = "{\"name\": \"X\", \"category\": \"Home\", \"price\": 9.99}";
        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.stock").value("stock is required"))
                .andExpect(jsonPath("$.errors.rating").value("rating is required"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deletingUnknownProductReturns404() throws Exception {
        mockMvc.perform(delete("/api/products/9999")).andExpect(status().isNotFound());
    }

    @Test
    void userCannotChangeProducts() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PRODUCT))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PRODUCT))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/products/1")).andExpect(status().isForbidden());
    }
}
