package com.example.app.link;

import java.time.Instant;

public record ShortLinkResponse(String code, String shortUrl, String originalUrl, Instant expiresAt, Instant createdAt) {}
