package com.everytldr.ingestor.source;

import java.time.Instant;

public record CollectedArticle(
    String contentUrl,
    String sourceName,
    String thumbnailUrl,
    String language,
    Instant publishedAt) {}
