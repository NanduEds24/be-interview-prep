package com.example.app.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class AuthDtos {

    private AuthDtos() {}

    // BCrypt rejects passwords over 72 bytes, hence the upper limits (characters and UTF-8 bytes).
    public record RegisterRequest(
            @NotBlank(message = "email is required") @Email(message = "email must be a valid email address") String email,
            @NotBlank(message = "password is required")
            @Size(min = 8, max = 72, message = "password must be 8 to 72 characters")
            @MaxUtf8Bytes(value = 72, message = "password must be at most 72 bytes") String password) {}

    public record LoginRequest(
            @NotBlank(message = "email is required") String email,
            @NotBlank(message = "password is required") String password) {}

    public record TokenResponse(String token, String tokenType, long expiresIn) {}

    public record UserResponse(Long id, String email, Role role, Instant createdAt) {

        static UserResponse from(AppUser user) {
            return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt());
        }
    }
}
