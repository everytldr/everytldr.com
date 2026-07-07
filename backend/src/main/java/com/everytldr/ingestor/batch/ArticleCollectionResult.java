package com.everytldr.ingestor.batch;

import com.everytldr.ingestor.source.ArticleCollectionTarget;
import com.everytldr.ingestor.source.CollectedArticle;
import java.util.List;
import java.util.Objects;

public record ArticleCollectionResult(
    ArticleCollectionTarget target, List<CollectedArticle> collectedArticles) {
  public ArticleCollectionResult {
    Objects.requireNonNull(target, "target must not be null");
    collectedArticles =
        List.copyOf(
            Objects.requireNonNull(collectedArticles, "collectedArticles must not be null"));
  }
}
