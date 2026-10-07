package com.example.app.link;

import java.time.Instant;

public record LinkStatsResponse(
        String code, String originalUrl, long visitCount, Instant createdAt, Instant expiresAt) {

    static LinkStatsResponse from(ShortLink link) {
        return new LinkStatsResponse(
                link.getCode(), link.getOriginalUrl(), link.getVisitCount(), link.getCreatedAt(), link.getExpiresAt());
    }
}
