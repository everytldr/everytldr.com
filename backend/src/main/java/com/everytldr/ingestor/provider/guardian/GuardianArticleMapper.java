package com.everytldr.ingestor.provider.guardian;

import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.ingestor.provider.CollectedArticle;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Slf4j
public class GuardianArticleMapper {

  public List<CollectedArticle> map(GuardianSearchResponse response, ArticleSource source) {
    if (response == null || response.response() == null || response.response().results() == null) {
      return List.of();
    }

    return response.response().results().stream()
        .map(result -> mapResult(result, source))
        .flatMap(Optional::stream)
        .toList();
  }

  private Optional<CollectedArticle> mapResult(
      GuardianSearchResponse.Result result, ArticleSource source) {
    if (result == null) {
      log.warn("Skipping Guardian article result because result is null");
      return Optional.empty();
    }
    if (!StringUtils.hasText(result.webUrl())) {
      log.warn("Skipping Guardian article result because webUrl is missing");
      return Optional.empty();
    }
    if (!StringUtils.hasText(result.webPublicationDate())) {
      log.warn(
          "Skipping Guardian article result because webPublicationDate is missing. webUrl={}",
          result.webUrl());
      return Optional.empty();
    }

    Instant publishedAt;
    try {
      publishedAt = Instant.parse(result.webPublicationDate());
    } catch (DateTimeParseException e) {
      log.warn(
          "Skipping Guardian article result because webPublicationDate is invalid. webUrl={}, webPublicationDate={}",
          result.webUrl(),
          result.webPublicationDate());
      return Optional.empty();
    }

    return Optional.of(
        new CollectedArticle(
            result.webUrl(),
            source.getName(),
            result.fields() == null ? null : result.fields().thumbnail(),
            source.getLanguage(),
            publishedAt));
  }
}
