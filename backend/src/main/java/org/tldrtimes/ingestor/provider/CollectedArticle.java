package org.tldrtimes.ingestor.provider;

import java.time.Instant;

public record CollectedArticle(
    String sourceUrl,
    String sourceName,
    String thumbnailUrl,
    String language,
    Instant publishedAt) {}
