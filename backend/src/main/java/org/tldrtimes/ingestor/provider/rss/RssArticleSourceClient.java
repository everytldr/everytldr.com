package org.tldrtimes.ingestor.provider.rss;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import java.io.StringReader;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.tldrtimes.common.domain.source.ArticleSource;
import org.tldrtimes.common.domain.source.SourceType;
import org.tldrtimes.ingestor.provider.ArticleSourceClient;
import org.tldrtimes.ingestor.provider.CollectedArticle;

@Component
@Profile("ingestor")
@Slf4j
public class RssArticleSourceClient implements ArticleSourceClient {

  private final RestClient restClient;

  public RssArticleSourceClient(RestClient.Builder restClientBuilder) {
    this.restClient = restClientBuilder.build();
  }

  @Override
  public boolean supports(SourceType sourceType) {
    return sourceType == SourceType.RSS;
  }

  @Override
  public List<CollectedArticle> collect(ArticleSource source) {
    String xml = restClient.get().uri(source.getUrl()).retrieve().body(String.class);
    SyndFeed feed = parse(xml, source);

    return feed.getEntries().stream()
        .map(entry -> mapEntry(entry, source))
        .flatMap(Optional::stream)
        .toList();
  }

  private SyndFeed parse(String xml, ArticleSource source) {
    if (!StringUtils.hasText(xml)) {
      throw new IllegalStateException(
          "RSS feed response is empty. sourceName=%s".formatted(source.getName()));
    }

    try {
      return new SyndFeedInput().build(new StringReader(xml));
    } catch (IllegalArgumentException | FeedException e) {
      throw new IllegalStateException(
          "Failed to parse RSS feed. sourceName=%s, sourceUrl=%s"
              .formatted(source.getName(), source.getUrl()),
          e);
    }
  }

  private Optional<CollectedArticle> mapEntry(SyndEntry entry, ArticleSource source) {
    if (entry == null) {
      log.warn("Skipping RSS entry because entry is null. sourceName={}", source.getName());
      return Optional.empty();
    }

    String link = entry.getLink();
    if (!StringUtils.hasText(link)) {
      log.warn("Skipping RSS entry because link is missing. sourceName={}", source.getName());
      return Optional.empty();
    }

    Instant publishedAt = resolvePublishedAt(entry);
    if (publishedAt == null) {
      log.warn("Skipping RSS entry because published date is missing. link={}", link);
      return Optional.empty();
    }

    return Optional.of(
        new CollectedArticle(link, source.getName(), null, source.getLanguage(), publishedAt));
  }

  private Instant resolvePublishedAt(SyndEntry entry) {
    Date publishedDate = entry.getPublishedDate();
    if (publishedDate != null) {
      return publishedDate.toInstant();
    }

    Date updatedDate = entry.getUpdatedDate();
    if (updatedDate != null) {
      return updatedDate.toInstant();
    }

    return null;
  }
}
