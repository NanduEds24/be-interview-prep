package com.example.app.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ShortLinkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShortLinkRepository repository;

    private String shorten(String url) throws Exception {
        String json = mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"" + url + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").isString())
                .andExpect(jsonPath("$.shortUrl").isString())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.code");
    }

    @Test
    void shortensRedirectsAndCountsVisits() throws Exception {
        String code = shorten("https://spring.io/projects");
        assertThat(code).matches("[A-Za-z0-9]{7}");

        mockMvc.perform(get("/r/{code}", code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://spring.io/projects"));
        mockMvc.perform(get("/r/{code}", code)).andExpect(status().isFound());

        mockMvc.perform(get("/api/links/{code}/stats", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalUrl").value("https://spring.io/projects"))
                .andExpect(jsonPath("$.visitCount").value(2))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void sameUrlTwiceGivesTwoCodes() throws Exception {
        assertThat(shorten("https://example.com")).isNotEqualTo(shorten("https://example.com"));
    }

    @Test
    void invalidUrlReturns400() throws Exception {
        // Includes URLs java.net.URL accepts but java.net.URI rejects: they used to pass and then 500 on redirect.
        for (String url : new String[] {"not a url", "ftp://example.com", "https://example.com/a b", "https://example.com/?q={x}"}) {
            mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON).content("{\"url\": \"" + url + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.url").value("url must be a valid http or https URL"));
        }
    }

    @Test
    void unusualButValidHostsAreAccepted() throws Exception {
        for (String url : new String[] {"https://my_service.example.com/x", "https://bücher.de/"}) {
            String code = shorten(url);
            mockMvc.perform(get("/r/{code}", code)).andExpect(status().isFound());
        }
    }

    @Test
    void nonAsciiPathIsPercentEncodedInLocation() throws Exception {
        String code = shorten("https://example.com/café");

        mockMvc.perform(get("/r/{code}", code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/caf%C3%A9"));
    }

    @Test
    void missingOrTooLongUrlReturns400() throws Exception {
        mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.url").value("url is required"));
        String longUrl = "https://example.com/" + "a".repeat(2048);
        mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON).content("{\"url\": \"" + longUrl + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.url").value("url must be at most 2048 characters"));
    }

    @Test
    void headRequestsRedirectWithoutCountingVisits() throws Exception {
        String code = shorten("https://example.com");

        mockMvc.perform(head("/r/{code}", code)).andExpect(status().isFound());
        mockMvc.perform(get("/api/links/{code}/stats", code)).andExpect(jsonPath("$.visitCount").value(0));
    }

    @Test
    void pastExpiryReturns400() throws Exception {
        mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://example.com\", \"expiresAt\": \"2000-01-01T00:00:00Z\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.expiresAt").value("expiresAt must be in the future"));
    }

    @Test
    void unknownCodeReturns404() throws Exception {
        mockMvc.perform(get("/r/nope123"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Short link nope123 not found"));
        mockMvc.perform(get("/api/links/nope123/stats")).andExpect(status().isNotFound());
    }

    @Test
    void expiredCodeReturns410AndIsNotCounted() throws Exception {
        repository.save(new ShortLink("old1", "https://example.com", Instant.now().minusSeconds(60)));

        mockMvc.perform(get("/r/old1"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.status").value(410));
        mockMvc.perform(get("/api/links/old1/stats")).andExpect(jsonPath("$.visitCount").value(0));
    }
}
