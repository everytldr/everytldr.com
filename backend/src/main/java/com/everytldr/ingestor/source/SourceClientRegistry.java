package com.everytldr.ingestor.source;

import com.everytldr.common.domain.source.SourceType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SourceClientRegistry {

  private final List<SourceClient> sourceClients;

  public SourceClient getClient(SourceType sourceType) {
    List<SourceClient> clients =
        sourceClients.stream().filter(client -> client.supports(sourceType)).toList();
    if (clients.size() == 1) {
      return clients.getFirst();
    }
    if (clients.size() > 1) {
      throw new IllegalStateException(
          "Multiple SourceClients support sourceType: %s".formatted(sourceType));
    }
    throw new IllegalStateException(
        "No SourceClient supports sourceType: %s".formatted(sourceType));
  }
}
