package org.tldrtimes.api.article;

import java.time.Instant;
import org.tldrtimes.common.domain.article.ArticleListProjection;

public record ArticleListItem(
    String id,
    String title,
    String summary,
    String thumbnailUrl,
    Instant publishedAt,
    String source,
    String category) {
  static ArticleListItem from(ArticleListProjection projection) {
    return new ArticleListItem(
        projection.id().toString(),
        projection.title(),
        projection.summary(),
        projection.thumbnailUrl(),
        projection.publishedAt(),
        projection.source(),
        projection.categorySlug());
  }
}
