package com.example.app.auth;

import com.example.app.common.MaxUtf8Bytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class AuthDtos {

    private AuthDtos() {}

    // BCrypt rejects passwords over 72 bytes. One byte limit (not @Size max) so there is only one error message.
    public record RegisterRequest(
            @NotBlank(message = "email is required") @Email(message = "email must be a valid email address")
            String email,

            @NotBlank(message = "password is required")
            @Size(min = 8, message = "password must be at least 8 characters")
            @MaxUtf8Bytes(value = SecurityConfig.MAX_PASSWORD_BYTES, message = "password must be at most 72 bytes")
            String password) {}

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
