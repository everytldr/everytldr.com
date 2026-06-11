package com.everytldr.enricher.content;

import com.everytldr.common.domain.article.Article;

public interface ContentResolver {
  boolean supports(Article article);

  String resolve(Article article);
}
