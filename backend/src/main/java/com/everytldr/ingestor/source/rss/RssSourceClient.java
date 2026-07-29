package com.everytldr.ingestor.source.rss;

import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.SourceType;
import com.everytldr.ingestor.ingestion.IngestionExceptions;
import com.everytldr.ingestor.source.ArticleCollectionTarget;
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
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
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
  public List<CollectedArticle> collect(ArticleCollectionTarget target) {
    Objects.requireNonNull(target, "target must not be null");
    ArticleSource source = target.source();
    String feedUrl = target.feedUrl();
    try {
      return fetchFeed(feedUrl, source);
    } catch (RestClientException e) {
      throw new IngestionExceptions.Retryable(
          "Failed to fetch RSS feed. sourceName=%s, feedUrl=%s"
              .formatted(source.getName(), feedUrl),
          e);
    } catch (IllegalArgumentException | IllegalStateException e) {
      throw new IngestionExceptions.Skippable(
          "Failed to read RSS feed. sourceName=%s, feedUrl=%s".formatted(source.getName(), feedUrl),
          e);
    }
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
      return new SyndFeedInput().build(new StringReader(FeedDateNormalizer.normalize(xml)));
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

    String contentUrl = resolveContentUrl(entry);
    if (contentUrl == null) {
      log.warn("Skipping RSS entry because link is missing. sourceName={}", source.getName());
      return Optional.empty();
    }
    if (!source.getPolicy().crawling().isAllowedContentUrl(contentUrl)) {
      log.warn(
          "Skipping RSS entry because link is not allowed. sourceName={}, link={}",
          source.getName(),
          contentUrl);
      return Optional.empty();
    }

    Instant publishedAt = resolvePublishedAt(entry);
    if (publishedAt == null) {
      log.warn("Skipping RSS entry because published date is missing. link={}", contentUrl);
      return Optional.empty();
    }

    String thumbnailUrl = resolveThumbnailUrl(entry);
    return Optional.of(
        new CollectedArticle(
            contentUrl,
            source.getName(),
            thumbnailUrl,
            source.getLanguage(),
            publishedAt,
            source.getLicenseInfo()));
  }

  private String resolveContentUrl(SyndEntry entry) {
    String link = entry.getLink();
    if (StringUtils.hasText(link)) {
      return link;
    }

    String uri = entry.getUri();
    return isHttpUrl(uri) ? uri : null;
  }

  private boolean isHttpUrl(String value) {
    if (!StringUtils.hasText(value)) {
      return false;
    }

    try {
      String scheme = URI.create(value).getScheme();
      return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    } catch (IllegalArgumentException e) {
      return false;
    }
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
