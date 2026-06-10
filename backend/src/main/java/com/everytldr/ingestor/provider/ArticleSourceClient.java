package com.everytldr.ingestor.provider;

import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.SourceType;
import java.util.List;

public interface ArticleSourceClient {
  boolean supports(SourceType sourceType);

  List<CollectedArticle> collect(ArticleSource source);
}
