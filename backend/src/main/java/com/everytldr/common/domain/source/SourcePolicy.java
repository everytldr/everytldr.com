package com.everytldr.common.domain.source;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.List;
import java.util.Objects;

public record SourcePolicy(CrawlingPolicy crawling) {
  public SourcePolicy {
    Objects.requireNonNull(crawling, "crawling must not be null");
  }

  public record CrawlingPolicy(
      @JsonProperty("feed_urls") List<String> feedUrls,
      List<String> hosts,
      @JsonProperty("content_selectors") List<String> contentSelectors,
      @JsonProperty("thumbnail_selectors") List<String> thumbnailSelectors,
      @JsonProperty("allowed_path_prefixes") List<String> allowedPathPrefixes) {
    public CrawlingPolicy {
      if (feedUrls == null || feedUrls.isEmpty()) {
        throw new IllegalArgumentException("feedUrls must not be empty");
      }
      if (hosts == null || hosts.isEmpty()) {
        throw new IllegalArgumentException("hosts must not be empty");
      }
      if (contentSelectors == null || contentSelectors.isEmpty()) {
        throw new IllegalArgumentException("contentSelectors must not be empty");
      }
      feedUrls = List.copyOf(feedUrls);
      hosts = List.copyOf(hosts);
      contentSelectors = List.copyOf(contentSelectors);
      thumbnailSelectors = List.copyOf(Objects.requireNonNullElse(thumbnailSelectors, List.of()));
      List<String> pathPrefixes = Objects.requireNonNullElse(allowedPathPrefixes, List.of());
      if (pathPrefixes.stream()
          .anyMatch(prefix -> prefix == null || prefix.isBlank() || !prefix.startsWith("/"))) {
        throw new IllegalArgumentException("allowedPathPrefixes must contain only absolute paths");
      }
      allowedPathPrefixes = List.copyOf(pathPrefixes);
    }

    public boolean isAllowedHost(String host) {
      if (host == null || host.isBlank()) {
        return false;
      }
      return hosts.stream().anyMatch(allowed -> allowed.equalsIgnoreCase(host));
    }

    public boolean isAllowedContentUrl(String contentUrl) {
      if (contentUrl == null || contentUrl.isBlank()) {
        return false;
      }

      try {
        return isAllowedContentUri(URI.create(contentUrl));
      } catch (IllegalArgumentException e) {
        return false;
      }
    }

    public boolean isAllowedContentUri(URI uri) {
      if (uri == null) {
        return false;
      }

      String scheme = uri.getScheme();
      boolean isHttpUrl = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
      return isHttpUrl && isAllowedHost(uri.getHost()) && isAllowedPath(uri.getPath());
    }

    private boolean isAllowedPath(String path) {
      if (allowedPathPrefixes.isEmpty()) {
        return true;
      }
      if (path == null || path.isBlank()) {
        return false;
      }
      return allowedPathPrefixes.stream().anyMatch(path::startsWith);
    }
  }
}
