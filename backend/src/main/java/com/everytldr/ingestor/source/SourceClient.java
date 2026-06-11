package com.everytldr.ingestor.source;

import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.SourceType;
import java.util.List;

public interface SourceClient {
  boolean supports(SourceType sourceType);

  List<CollectedArticle> collect(ArticleSource source);
}
