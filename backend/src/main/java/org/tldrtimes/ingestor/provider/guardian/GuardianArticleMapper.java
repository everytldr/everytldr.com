package org.tldrtimes.ingestor.provider.guardian;

import java.time.Instant;
import java.util.List;
import org.tldrtimes.common.domain.source.ArticleSource;
import org.tldrtimes.ingestor.provider.CollectedArticle;

public class GuardianArticleMapper {

  public List<CollectedArticle> map(GuardianSearchResponse response, ArticleSource source) {
    return response.response().results().stream()
        .map(
            result ->
                new CollectedArticle(
                    result.webUrl(),
                    source.getName(),
                    result.fields() == null ? null : result.fields().thumbnail(),
                    source.getLanguage(),
                    Instant.parse(result.webPublicationDate())))
        .toList();
  }
}
