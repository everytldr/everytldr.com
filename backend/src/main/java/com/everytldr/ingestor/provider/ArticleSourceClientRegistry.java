package com.everytldr.ingestor.provider;

import com.everytldr.common.domain.source.SourceType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArticleSourceClientRegistry {

  private final List<ArticleSourceClient> articleSourceClients;

  public ArticleSourceClient getClient(SourceType sourceType) {
    List<ArticleSourceClient> clients =
        articleSourceClients.stream().filter(client -> client.supports(sourceType)).toList();
    if (clients.size() == 1) {
      return clients.getFirst();
    }
    if (clients.size() > 1) {
      throw new IllegalStateException(
          "Multiple ArticleSourceClients support sourceType: %s".formatted(sourceType));
    }
    throw new IllegalStateException(
        "No ArticleSourceClient supports sourceType: %s".formatted(sourceType));
  }
}
