package org.tldrtimes.ingestor.provider.guardian;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;
import org.tldrtimes.common.domain.source.ArticleSource;
import org.tldrtimes.ingestor.provider.CollectedArticle;

@Component
public class GuardianArticleMapper {

  public List<CollectedArticle> map(GuardianSearchResponse response, ArticleSource source) {
    if (response == null || response.response() == null || response.response().results() == null) {
      return List.of();
    }

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
