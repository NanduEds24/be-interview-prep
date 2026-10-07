package com.example.app.auth;

import com.example.app.auth.AuthDtos.LoginRequest;
import com.example.app.auth.AuthDtos.RegisterRequest;
import com.example.app.auth.AuthDtos.TokenResponse;
import com.example.app.auth.AuthDtos.UserResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final UserService service;

    public AuthController(UserService service) {
        this.service = service;
    }

    @PostMapping("/api/auth/register")
    @SecurityRequirements // public: no token needed (no lock in Swagger UI)
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return service.register(request);
    }

    @PostMapping("/api/auth/login")
    @SecurityRequirements
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    /** The email comes from the verified token (set by JwtAuthenticationFilter), never from the request. */
    @GetMapping("/api/users/me")
    public UserResponse me(Authentication authentication) {
        return service.profile(authentication.getName());
    }

    /** ADMIN only: enforced in SecurityConfig. */
    @GetMapping("/api/users")
    public List<UserResponse> listUsers() {
        return service.listAll();
    }
}
