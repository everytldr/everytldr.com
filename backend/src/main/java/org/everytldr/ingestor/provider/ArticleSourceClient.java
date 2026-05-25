package org.everytldr.ingestor.provider;

import java.util.List;
import org.everytldr.common.domain.source.ArticleSource;
import org.everytldr.common.domain.source.SourceType;

public interface ArticleSourceClient {
  boolean supports(SourceType sourceType);

  List<CollectedArticle> collect(ArticleSource source);
}
