package org.tldrtimes.common.domain.article;

import java.time.Instant;

public record ArticleListProjection(
    Long id,
    String title,
    String content,
    String thumbnailUrl,
    Instant publishedAt,
    String source,
    String categorySlug) {}
