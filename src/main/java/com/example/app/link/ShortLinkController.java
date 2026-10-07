package com.example.app.link;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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
                .path("/r/{code}")
                .buildAndExpand(link.getCode())
                .toUriString();
        return ShortLinkResponse.from(link, shortUrl);
    }

    /** 302 (not 301) so browsers don't cache the redirect and every visit reaches us to be counted. */
    @GetMapping("/r/{code}")
    @SecurityRequirements // public: no token needed (shown without a lock in Swagger UI)
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        return found(service.resolve(code, true));
    }

    /** Same answer for HEAD (link checkers, chat previews), but not counted as a visit. */
    @RequestMapping(path = "/r/{code}", method = RequestMethod.HEAD)
    @SecurityRequirements
    public ResponseEntity<Void> redirectHead(@PathVariable String code) {
        return found(service.resolve(code, false));
    }

    @GetMapping("/api/links/{code}/stats")
    public LinkStatsResponse stats(@PathVariable String code) {
        return service.stats(code);
    }

    // ResponseEntity.location writes uri.toASCIIString(), so non-ASCII characters are percent-encoded.
    private static ResponseEntity<Void> found(String url) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }
}
