package com.example.app.link;

import java.security.SecureRandom;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ShortLinkService {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int CODE_LENGTH = 7;
    private static final int MAX_ATTEMPTS = 5;

    private final ShortLinkRepository repository;
    private final SecureRandom random = new SecureRandom();

    public ShortLinkService(ShortLinkRepository repository) {
        this.repository = repository;
    }

    /** Every call creates a new code, even for a URL seen before: each link keeps its own expiry and stats. */
    @Transactional
    public ShortLink shorten(ShortenRequest request) {
        return repository.save(new ShortLink(newCode(), request.url(), request.expiresAt()));
    }

    /**
     * Returns the original URL; 404 for unknown codes, 410 for expired ones. countVisit is false for HEAD
     * requests (link checkers, chat previews), which shouldn't inflate the stats.
     */
    @Transactional
    public String resolve(String code, boolean countVisit) {
        ShortLink link = find(code);
        // The UPDATE re-checks expiry itself, so a link that expires right after the check isn't counted.
        if (link.isExpired() || (countVisit && repository.incrementVisitCount(code, Instant.now()) == 0)) {
            throw new ResponseStatusException(HttpStatus.GONE, "Short link " + code + " has expired");
        }
        return link.getOriginalUrl();
    }

    @Transactional(readOnly = true)
    public LinkStatsResponse stats(String code) {
        return LinkStatsResponse.from(find(code));
    }

    private ShortLink find(String code) {
        return repository
                .findByCode(code)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Short link " + code + " not found"));
    }

    // 62^7 (about 3.5 trillion) codes make a collision rare; the unique column is the final guard.
    private String newCode() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            StringBuilder code = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
            if (!repository.existsByCode(code.toString())) {
                return code.toString();
            }
        }
        throw new IllegalStateException("Could not generate a unique short code");
    }
}
