package com.example.app.auth;

import com.example.app.auth.AuthDtos.LoginRequest;
import com.example.app.auth.AuthDtos.RegisterRequest;
import com.example.app.auth.AuthDtos.TokenResponse;
import com.example.app.auth.AuthDtos.UserResponse;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(AppUserRepository repository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Public registration always creates a USER; admins are only created by AdminSeeder. */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (repository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email " + email + " is already registered");
        }
        AppUser user = new AppUser(email, passwordEncoder.encode(request.password()), Role.USER);
        return UserResponse.from(repository.save(user));
    }

    /**
     * Same message for unknown email and wrong password, so the response doesn't say which part was wrong.
     * (It doesn't hide which emails exist: register answers 409 for those, as the spec requires.)
     */
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        AppUser user = repository
                .findByEmail(normalize(request.email()))
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return new TokenResponse(jwtService.createToken(user), "Bearer", JwtService.TOKEN_TTL.toSeconds());
    }

    @Transactional(readOnly = true)
    public UserResponse profile(String email) {
        return repository
                .findByEmail(email)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User " + email + " not found"));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listAll() {
        return repository.findAllByOrderByIdAsc().stream()
                .map(UserResponse::from)
                .toList();
    }

    static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
