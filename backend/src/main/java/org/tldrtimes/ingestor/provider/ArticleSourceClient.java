package org.tldrtimes.ingestor.provider;

import java.util.List;
import org.tldrtimes.common.domain.source.ArticleSource;
import org.tldrtimes.common.domain.source.SourceType;

public interface ArticleSourceClient {
  boolean supports(SourceType sourceType);

  List<CollectedArticle> collect(ArticleSource source);
}
