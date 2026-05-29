package org.everytldr.enricher.enrichment;

import org.everytldr.common.domain.article.Article;

public interface ArticleContentResolver {
  boolean supports(Article article);

  ArticleContent resolve(Article article);
}
