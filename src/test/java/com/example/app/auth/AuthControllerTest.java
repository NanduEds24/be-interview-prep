package com.example.app.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private void register(String email, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    private String login(String email, String password) throws Exception {
        String json = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    private String adminToken() throws Exception {
        repository.save(new AppUser("admin@test.com", passwordEncoder.encode("admin-pass-123"), Role.ADMIN));
        return login("admin@test.com", "admin-pass-123");
    }

    @Test
    void registerLoginAndViewOwnProfile() throws Exception {
        register("alice@test.com", "password123");
        String token = login("alice@test.com", "password123");

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@test.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void passwordIsStoredAsBcryptHash() throws Exception {
        register("hash@test.com", "password123");

        String hash = repository.findByEmail("hash@test.com").orElseThrow().getPasswordHash();
        assertThat(hash).isNotEqualTo("password123").startsWith("$2a$");
    }

    @Test
    void userCannotListAllUsers() throws Exception {
        register("bob@test.com", "password123");
        String token = login("bob@test.com", "password123");

        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    @Test
    void adminCanListAllUsers() throws Exception {
        register("carol@test.com", "password123");

        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.email == 'carol@test.com')]").exists());
    }

    @Test
    void missingTokenReturns401Json() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
        mockMvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());
    }

    @Test
    void tamperedTokenReturns401() throws Exception {
        register("dave@test.com", "password123");
        String token = login("dave@test.com", "password123");

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token + "x"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenExpiresAfter15Minutes() throws Exception {
        register("erin@test.com", "password123");
        AppUser user = repository.findByEmail("erin@test.com").orElseThrow();
        String oldToken = jwtService.createToken(user, Instant.now().minusSeconds(15 * 60 + 1));

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + oldToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordReturns401WithoutHint() throws Exception {
        register("frank@test.com", "password123");

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"frank@test.com\", \"password\": \"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void duplicateEmailReturns409() throws Exception {
        register("gina@test.com", "password123");

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"GINA@test.com\", \"password\": \"password123\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void invalidRegistrationReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"not-an-email\", \"password\": \"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").value("email must be a valid email address"))
                .andExpect(jsonPath("$.errors.password").value("password must be 8 to 72 characters"));
    }
}
