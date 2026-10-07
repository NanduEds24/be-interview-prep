package com.example.app.auth;

import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the first ADMIN from the ADMIN_EMAIL and ADMIN_PASSWORD environment variables, if both are set. */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminSeeder(AppUserRepository repository, PasswordEncoder passwordEncoder,
            @Value("${app.admin.email:}") String email, @Value("${app.admin.password:}") String password) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            return;
        }
        int bytes = password.getBytes(StandardCharsets.UTF_8).length;
        if (password.length() < 8 || bytes > SecurityConfig.MAX_PASSWORD_BYTES) {
            throw new IllegalStateException("ADMIN_PASSWORD must be at least 8 characters and at most 72 bytes");
        }
        String normalized = UserService.normalize(email);
        repository.findByEmail(normalized).ifPresentOrElse(
                // Never promote an existing account: whoever registered it chose its password.
                existing -> {
                    if (existing.getRole() != Role.ADMIN) {
                        log.warn("ADMIN_EMAIL {} is already registered as {}; no admin was created", normalized,
                                existing.getRole());
                    } else {
                        log.info("Admin {} already exists; ADMIN_PASSWORD is only used when it is first created",
                                normalized);
                    }
                },
                () -> repository.save(new AppUser(normalized, passwordEncoder.encode(password), Role.ADMIN)));
    }
}
