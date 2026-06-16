package com.everytldr.ingestor.source.rss;

import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.SourcePolicy.ThumbnailPolicy;
import com.everytldr.common.domain.source.SourceType;
import com.everytldr.ingestor.source.CollectedArticle;
import com.everytldr.ingestor.source.SourceClient;
import com.rometools.modules.mediarss.MediaEntryModule;
import com.rometools.modules.mediarss.MediaModule;
import com.rometools.modules.mediarss.types.Metadata;
import com.rometools.modules.mediarss.types.Reference;
import com.rometools.modules.mediarss.types.Thumbnail;
import com.rometools.modules.mediarss.types.UrlReference;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import java.io.StringReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Profile("ingestor")
@Slf4j
public class RssSourceClient implements SourceClient {

  private final RestClient restClient;

  public RssSourceClient(RestClient.Builder restClientBuilder, FeedProperties feedProperties) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(feedProperties.connectTimeout());
    requestFactory.setReadTimeout(feedProperties.readTimeout());
    this.restClient = restClientBuilder.requestFactory(requestFactory).build();
  }

  @Override
  public boolean supports(SourceType sourceType) {
    return sourceType == SourceType.RSS;
  }

  @Override
  public List<CollectedArticle> collect(ArticleSource source) {
    List<String> feedUrls = source.getPolicy().crawling().feedUrls();
    List<CollectedArticle> collected = new ArrayList<>();
    int failedFeeds = 0;

    for (String feedUrl : feedUrls) {
      try {
        collected.addAll(fetchFeed(feedUrl, source));
      } catch (RestClientException | IllegalStateException e) {
        failedFeeds++;
        log.warn(
            "Failed to fetch RSS feed. sourceName={}, feedUrl={}", source.getName(), feedUrl, e);
      }
    }

    log.info(
        "Fetched RSS feeds for source. sourceName={}, feeds={}, failedFeeds={}, collected={}",
        source.getName(),
        feedUrls.size(),
        failedFeeds,
        collected.size());

    if (failedFeeds == feedUrls.size()) {
      throw new IllegalStateException(
          "All RSS feeds failed. sourceName=%s, feeds=%d"
              .formatted(source.getName(), feedUrls.size()));
    }

    return collected;
  }

  private List<CollectedArticle> fetchFeed(String feedUrl, ArticleSource source) {
    String xml = restClient.get().uri(feedUrl).retrieve().body(String.class);
    SyndFeed feed = parse(xml, source, feedUrl);

    return feed.getEntries().stream()
        .map(entry -> mapEntry(entry, source))
        .flatMap(Optional::stream)
        .toList();
  }

  private SyndFeed parse(String xml, ArticleSource source, String feedUrl) {
    if (!StringUtils.hasText(xml)) {
      throw new IllegalStateException(
          "RSS feed response is empty. sourceName=%s, feedUrl=%s"
              .formatted(source.getName(), feedUrl));
    }

    try {
      return new SyndFeedInput().build(new StringReader(xml));
    } catch (IllegalArgumentException | FeedException e) {
      throw new IllegalStateException(
          "Failed to parse RSS feed. sourceName=%s, feedUrl=%s"
              .formatted(source.getName(), feedUrl),
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

    String thumbnailUrl = resolveThumbnailUrl(entry, source);
    return Optional.of(
        new CollectedArticle(
            link, source.getName(), thumbnailUrl, source.getLanguage(), publishedAt));
  }

  private String resolveThumbnailUrl(SyndEntry entry, ArticleSource source) {
    if (source.getPolicy().eligibility().thumbnailPolicy() != ThumbnailPolicy.ALLOW) {
      return null;
    }
    return resolveThumbnailUrl(entry);
  }

  private String resolveThumbnailUrl(SyndEntry entry) {
    MediaEntryModule media = (MediaEntryModule) entry.getModule(MediaModule.URI);

    String thumbnailUrl = findThumbnailUrl(media);
    if (thumbnailUrl != null) {
      return thumbnailUrl;
    }

    String imageContentUrl = findImageContentUrl(media);
    if (imageContentUrl != null) {
      return imageContentUrl;
    }

    return findImageEnclosureUrl(entry);
  }

  private String findThumbnailUrl(MediaEntryModule media) {
    Metadata metadata = media == null ? null : media.getMetadata();
    if (metadata == null || metadata.getThumbnail() == null) {
      return null;
    }
    return Arrays.stream(metadata.getThumbnail())
        .map(Thumbnail::getUrl)
        .filter(url -> url != null)
        .map(Object::toString)
        .findFirst()
        .orElse(null);
  }

  private String findImageContentUrl(MediaEntryModule media) {
    if (media == null || media.getMediaContents() == null) {
      return null;
    }
    return Arrays.stream(media.getMediaContents())
        .filter(
            content -> {
              String type = content.getType();
              boolean isImage =
                  "image".equalsIgnoreCase(content.getMedium())
                      || (type != null && type.toLowerCase().startsWith("image/"));
              return isImage;
            })
        .map(
            content -> {
              Reference reference = content.getReference();
              if (!(reference instanceof UrlReference urlReference)
                  || urlReference.getUrl() == null) {
                return null;
              }
              String contentUrl = urlReference.getUrl().toString();
              return contentUrl;
            })
        .filter(StringUtils::hasText)
        .findFirst()
        .orElse(null);
  }

  private String findImageEnclosureUrl(SyndEntry entry) {
    return entry.getEnclosures().stream()
        .filter(
            enclosure -> {
              String type = enclosure.getType();
              boolean isImage = type != null && type.toLowerCase().startsWith("image/");
              return isImage;
            })
        .map(SyndEnclosure::getUrl)
        .filter(StringUtils::hasText)
        .findFirst()
        .orElse(null);
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
