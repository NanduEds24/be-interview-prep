package com.example.app.link;

import jakarta.validation.Valid;
import java.net.URI;
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
        return new ShortLinkResponse(link.getCode(), shortUrl, link.getOriginalUrl(), link.getExpiresAt(),
                link.getCreatedAt());
    }

    /** 302 (not 301) so browsers don't cache the redirect and every visit reaches us to be counted. */
    @GetMapping("/r/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(service.resolve(code))).build();
    }

    @GetMapping("/api/links/{code}/stats")
    public LinkStatsResponse stats(@PathVariable String code) {
        return service.stats(code);
    }
}
