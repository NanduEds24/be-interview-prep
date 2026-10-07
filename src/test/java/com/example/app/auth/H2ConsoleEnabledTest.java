package com.example.app.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

/** With H2_CONSOLE_ENABLED=true the console must actually open (no token) and be allowed in a frame. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.h2.console.enabled=true")
class H2ConsoleEnabledTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void consoleOpensWithoutTokenWhenEnabled() {
        ResponseEntity<String> response = restTemplate.getForEntity("/h2-console/", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getFirst("X-Frame-Options")).isEqualTo("SAMEORIGIN");
    }
}
