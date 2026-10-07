package com.example.app.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AdminSeederTest {

    @Test
    void rejectsAdminPasswordBcryptCannotStore() {
        // The check runs before the repository or encoder is used, so they can be null here.
        assertThatThrownBy(() -> new AdminSeeder(null, null, "admin@test.com", "short").run(null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("ADMIN_PASSWORD");
        assertThatThrownBy(() -> new AdminSeeder(null, null, "admin@test.com", "a".repeat(73)).run(null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("ADMIN_PASSWORD");
    }
}
