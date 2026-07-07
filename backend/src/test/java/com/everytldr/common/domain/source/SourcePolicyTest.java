package com.everytldr.common.domain.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.everytldr.common.domain.source.SourcePolicy.CrawlingPolicy;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class SourcePolicyTest {

  @Test
  void allowsExactConfiguredHostsIgnoringCase() {
    CrawlingPolicy policy = policy("news.example.com");

    assertThat(policy.isAllowedContentUrl("https://news.example.com/story")).isTrue();
    assertThat(policy.isAllowedContentUrl("https://NEWS.example.com/story")).isTrue();
    assertThat(policy.isAllowedContentUri(URI.create("http://news.example.com/story"))).isTrue();
  }

  @Test
  void rejectsDisallowedContentUrls() {
    CrawlingPolicy policy = policy("news.example.com");

    assertThat(policy.isAllowedContentUrl("https://cdn.news.example.com/story")).isFalse();
    assertThat(policy.isAllowedContentUrl("https://example.com/story")).isFalse();
    assertThat(policy.isAllowedContentUrl("not a url")).isFalse();
    assertThat(policy.isAllowedContentUrl("/relative/story")).isFalse();
    assertThat(policy.isAllowedContentUrl("ftp://news.example.com/story")).isFalse();
    assertThat(policy.isAllowedContentUrl(null)).isFalse();
    assertThat(policy.isAllowedContentUrl(" ")).isFalse();
  }

  @Test
  void allowsOnlyConfiguredContentPathPrefixesWhenPresent() {
    CrawlingPolicy policy =
        new CrawlingPolicy(
            List.of("https://news.example.com/feed.xml"),
            List.of("news.example.com"),
            List.of("article"),
            List.of(),
            List.of("/global/news/", "/global/features/"));

    assertThat(policy.isAllowedContentUrl("https://news.example.com/global/news/story")).isTrue();
    assertThat(policy.isAllowedContentUrl("https://news.example.com/global/features/story"))
        .isTrue();
    assertThat(policy.isAllowedContentUrl("https://news.example.com/global/podcast/story"))
        .isFalse();
    assertThat(
            policy.isAllowedContentUrl("https://news.example.com/global/supported-content/story"))
        .isFalse();
    assertThat(policy.isAllowedContentUrl("https://partner.example.com/global/news/story"))
        .isFalse();
    assertThat(policy.isAllowedContentUrl("ftp://news.example.com/global/news/story")).isFalse();
    assertThat(policy.isAllowedContentUrl("/global/news/story")).isFalse();
  }

  @Test
  void acceptsHttpAndHttpsFeedUrls() {
    CrawlingPolicy policy =
        policyWithFeedUrls(
            List.of("https://news.example.com/feed.xml", "http://feeds.news.example.com/rss.xml"));

    assertThat(policy.feedUrls())
        .containsExactly(
            "https://news.example.com/feed.xml", "http://feeds.news.example.com/rss.xml");
  }

  @Test
  void rejectsInvalidFeedUrls() {
    assertThatThrownBy(() -> policyWithFeedUrls(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("feedUrls");
    assertThatThrownBy(() -> policyWithFeedUrls(List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("feedUrls");
    assertThatThrownBy(() -> policyWithFeedUrls(Collections.singletonList(null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("feedUrls");

    for (String feedUrl :
        List.of(
            " ",
            "/relative-feed.xml",
            "ftp://news.example.com/feed.xml",
            "https:///feed.xml",
            "https:/news.example.com/feed.xml",
            "not a url")) {
      assertThatThrownBy(() -> policyWithFeedUrls(List.of(feedUrl)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("feedUrls");
    }
  }

  private CrawlingPolicy policy(String host) {
    return policyWithFeedUrlsAndHost(List.of("https://news.example.com/feed.xml"), host);
  }

  private CrawlingPolicy policyWithFeedUrls(List<String> feedUrls) {
    return policyWithFeedUrlsAndHost(feedUrls, "news.example.com");
  }

  private CrawlingPolicy policyWithFeedUrlsAndHost(List<String> feedUrls, String host) {
    return new CrawlingPolicy(feedUrls, List.of(host), List.of("article"), List.of(), List.of());
  }
}
