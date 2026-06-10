package com.everytldr.enricher.enrichment;

import com.everytldr.common.domain.article.Article;

public interface ArticleContentResolver {
  boolean supports(Article article);

  ArticleContent resolve(Article article);
}
