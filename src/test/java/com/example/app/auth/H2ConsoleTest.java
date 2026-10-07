package com.example.app.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

/**
 * Calls the real server: the H2 console is its own servlet, which MockMvc never reaches. Skipped when
 * the developer has turned the console on for debugging, since then it is meant to answer.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisabledIfEnvironmentVariable(named = "H2_CONSOLE_ENABLED", matches = "(?i)true")
class H2ConsoleTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void h2ConsoleIsOffByDefault() {
        HttpStatus status = HttpStatus.valueOf(restTemplate.getForEntity("/h2-console/", String.class).getStatusCode().value());
        // 401: no servlet answers, and /h2-console is not on the public list either.
        assertThat(status).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
