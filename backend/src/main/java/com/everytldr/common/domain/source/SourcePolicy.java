package com.everytldr.common.domain.source;

import java.util.List;
import java.util.Objects;

public record SourcePolicy(CrawlingPolicy crawling) {
  public SourcePolicy {
    Objects.requireNonNull(crawling, "crawling must not be null");
  }

  public record CrawlingPolicy(List<String> hosts, List<String> selectors) {
    public CrawlingPolicy {
      if (hosts == null || hosts.isEmpty()) {
        throw new IllegalArgumentException("hosts must not be empty");
      }
      if (selectors == null || selectors.isEmpty()) {
        throw new IllegalArgumentException("selectors must not be empty");
      }
      hosts = List.copyOf(hosts);
      selectors = List.copyOf(selectors);
    }

    public boolean isAllowedHost(String host) {
      if (host == null || host.isBlank()) {
        return false;
      }
      return hosts.stream().anyMatch(allowed -> allowed.equalsIgnoreCase(host));
    }
  }
}
