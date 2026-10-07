package com.example.app.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** BCrypt's input limit. */
    static final int MAX_PASSWORD_BYTES = 72;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService, ObjectMapper objectMapper,
            @Value("${spring.h2.console.enabled:false}") boolean h2ConsoleEnabled) throws Exception {
        if (h2ConsoleEnabled) {
            // Local debugging only (H2_CONSOLE_ENABLED=true): the console can't send our token and runs in a frame.
            http.headers(h -> h.frameOptions(f -> f.sameOrigin()))
                    .authorizeHttpRequests(auth -> auth.requestMatchers(PathRequest.toH2Console()).permitAll());
        }
        return http
                // No cookies or sessions: the token is sent explicitly, so CSRF protection isn't needed.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/api/health", "/r/**", "/error").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, ex) ->
                                writeProblem(objectMapper, request, response, HttpStatus.UNAUTHORIZED,
                                        "Missing, invalid or expired token"))
                        .accessDeniedHandler((request, response, ex) ->
                                writeProblem(objectMapper, request, response, HttpStatus.FORBIDDEN,
                                        "You do not have permission to access this resource")))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder() {
            // BCrypt ignores everything after 72 bytes when checking, so "password + anything" would match.
            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return rawPassword != null
                        && rawPassword.toString().getBytes(StandardCharsets.UTF_8).length <= MAX_PASSWORD_BYTES
                        && super.matches(rawPassword, encodedPassword);
            }
        };
    }

    // Security errors happen before any controller runs, so ApiExceptionHandler can't format them; same JSON here.
    private static void writeProblem(ObjectMapper objectMapper, HttpServletRequest request,
            HttpServletResponse response, HttpStatus status, String detail) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setInstance(URI.create(request.getRequestURI()));
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
