package org.everytldr.common.domain.article;

import java.time.Instant;

public record ArticleListProjection(
    Long id,
    String title,
    String summary,
    String thumbnailUrl,
    Instant publishedAt,
    String source,
    String categorySlug) {}
