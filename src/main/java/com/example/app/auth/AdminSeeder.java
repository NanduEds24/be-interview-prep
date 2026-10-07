package com.example.app.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the first ADMIN from the ADMIN_EMAIL and ADMIN_PASSWORD environment variables, if both are set. */
@Component
public class AdminSeeder implements ApplicationRunner {

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
        String normalized = UserService.normalize(email);
        if (!repository.existsByEmail(normalized)) {
            repository.save(new AppUser(normalized, passwordEncoder.encode(password), Role.ADMIN));
        }
    }
}
