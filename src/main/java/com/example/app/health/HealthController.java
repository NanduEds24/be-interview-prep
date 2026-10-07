package com.example.app.health;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    public record HealthResponse(String status) {}

    @GetMapping
    @SecurityRequirements // public
    public HealthResponse health() {
        return new HealthResponse("OK");
    }
}
