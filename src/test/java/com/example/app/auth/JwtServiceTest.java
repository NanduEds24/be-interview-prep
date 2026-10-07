package com.example.app.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JwtServiceTest {

    @Test
    void shortSecretFailsWithClearMessage() {
        assertThatThrownBy(() -> new JwtService("changeme"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT_SECRET must be at least 32 bytes (it is 8)");
    }
}
