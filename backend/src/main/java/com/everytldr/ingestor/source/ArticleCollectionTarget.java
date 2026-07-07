package com.everytldr.ingestor.source;

import com.everytldr.common.domain.source.ArticleSource;
import java.util.Objects;

public record ArticleCollectionTarget(ArticleSource source, String feedUrl) {
  public ArticleCollectionTarget {
    Objects.requireNonNull(source, "source must not be null");
    Objects.requireNonNull(feedUrl, "feedUrl must not be null");
    if (feedUrl.isBlank()) {
      throw new IllegalArgumentException("feedUrl must not be blank");
    }
  }
}
