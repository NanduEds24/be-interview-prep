package com.example.app.link;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
public class ShortLinkController {

    private final ShortLinkService service;

    public ShortLinkController(ShortLinkService service) {
        this.service = service;
    }

    @PostMapping("/api/links")
    @ResponseStatus(HttpStatus.CREATED)
    public ShortLinkResponse shorten(@Valid @RequestBody ShortenRequest request) {
        ShortLink link = service.shorten(request);
        String shortUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/r/{code}").buildAndExpand(link.getCode()).toUriString();
        return ShortLinkResponse.from(link, shortUrl);
    }

    /**
     * 302 (not 301) so browsers don't cache the redirect and every visit reaches us to be counted.
     * Spring also routes HEAD here; those get the same answer but aren't counted as visits.
     */
    @GetMapping("/r/{code}")
    @SecurityRequirements // public: no token needed (shown without a lock in Swagger UI)
    public ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest request) {
        boolean countVisit = HttpMethod.GET.matches(request.getMethod());
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(service.resolve(code, countVisit))).build();
    }

    @GetMapping("/api/links/{code}/stats")
    public LinkStatsResponse stats(@PathVariable String code) {
        return service.stats(code);
    }
}
