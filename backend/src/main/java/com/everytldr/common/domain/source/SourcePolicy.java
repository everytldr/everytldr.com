package com.everytldr.common.domain.source;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

public record SourcePolicy(CrawlingPolicy crawling) {
  public SourcePolicy {
    Objects.requireNonNull(crawling, "crawling must not be null");
  }

  public record CrawlingPolicy(
      List<String> hosts,
      @JsonProperty("content_selectors") List<String> contentSelectors,
      @JsonProperty("thumbnail_selectors") List<String> thumbnailSelectors) {
    public CrawlingPolicy {
      if (hosts == null || hosts.isEmpty()) {
        throw new IllegalArgumentException("hosts must not be empty");
      }
      if (contentSelectors == null || contentSelectors.isEmpty()) {
        throw new IllegalArgumentException("contentSelectors must not be empty");
      }
      hosts = List.copyOf(hosts);
      contentSelectors = List.copyOf(contentSelectors);
      thumbnailSelectors = List.copyOf(Objects.requireNonNullElse(thumbnailSelectors, List.of()));
    }

    public boolean isAllowedHost(String host) {
      if (host == null || host.isBlank()) {
        return false;
      }
      return hosts.stream().anyMatch(allowed -> allowed.equalsIgnoreCase(host));
    }
  }
}
