package com.everytldr.ingestor.provider;

import java.util.List;
import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.SourceType;

public interface ArticleSourceClient {
  boolean supports(SourceType sourceType);

  List<CollectedArticle> collect(ArticleSource source);
}
