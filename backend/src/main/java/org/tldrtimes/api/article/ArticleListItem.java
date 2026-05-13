package org.tldrtimes.api.article;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import org.tldrtimes.common.domain.article.ArticleListProjection;

public record ArticleListItem(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String summary,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String thumbnailUrl,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant publishedAt,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String source,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String category) {
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
