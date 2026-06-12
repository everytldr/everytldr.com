package com.everytldr.enricher.content;

import com.everytldr.common.domain.article.Article;
import java.util.Objects;

public interface ContentResolver {
  boolean supports(Article article);

  ResolvedArticle resolve(Article article);

  record ResolvedArticle(String content, String thumbnailUrl) {
    public ResolvedArticle {
      Objects.requireNonNull(content, "content must not be null");
    }
  }
}
