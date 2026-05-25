package org.everytldr.api.article;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import org.everytldr.common.domain.article.ArticleListProjection;

public record ArticleListItem(
    @Schema(requiredMode = RequiredMode.REQUIRED) String id,
    @Schema(requiredMode = RequiredMode.REQUIRED) String title,
    @Schema(requiredMode = RequiredMode.REQUIRED) String summary,
    @Schema(
            requiredMode = RequiredMode.REQUIRED,
            types = {"string", "null"}) // TODO: thumbnailUrl 나중에 Nullable 제거해야함
        String thumbnailUrl,
    @Schema(requiredMode = RequiredMode.REQUIRED) Instant publishedAt,
    @Schema(requiredMode = RequiredMode.REQUIRED) String source,
    @Schema(requiredMode = RequiredMode.REQUIRED) String category) {
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
