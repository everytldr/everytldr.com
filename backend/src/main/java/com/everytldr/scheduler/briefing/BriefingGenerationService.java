package com.everytldr.scheduler.briefing;

import com.everytldr.common.domain.article.ArticleRepository;
import com.everytldr.common.domain.article.ArticleRepository.ListItemProjection;
import com.everytldr.common.domain.briefing.BriefingRepository;
import com.everytldr.common.domain.language.SupportedLanguage;
import com.everytldr.common.domain.license.LicensePolicyEvaluator;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Profile("scheduler")
@ConditionalOnProperty(name = "everytldr.briefing.generation.enabled", havingValue = "true")
@Slf4j
public class BriefingGenerationService {
  static final int MIN_ARTICLE_COUNT = 3;
  private static final int SOURCE_FETCH_MULTIPLIER = 3;

  private final ArticleRepository articleRepository;
  private final BriefingRepository briefingRepository;
  private final LicensePolicyEvaluator licensePolicyEvaluator;
  private final BriefingGenerationClient generationClient;
  private final BriefingWriter briefingWriter;
  private final BriefingGenerationProperties properties;
  private final Clock clock;

  public void generateDailyBriefing() {
    LocalDate briefingDate = LocalDate.now(clock).minusDays(1);
    if (briefingRepository.existsByBriefingDate(briefingDate)) {
      return;
    }

    List<ListItemProjection> sources = findMostViewedSources(briefingDate);
    if (sources.size() < MIN_ARTICLE_COUNT) {
      log.warn(
          "Skipped briefing generation. briefingDate={}, sourceCount={}",
          briefingDate,
          sources.size());
      return;
    }

    BriefingGenerationClient.Request request = toRequest(sources);
    List<BriefingGenerationClient.Result> results = generationClient.generate(request);
    List<Long> articleIds = sources.stream().map(ListItemProjection::id).toList();
    briefingWriter.save(briefingDate, results, articleIds);
    log.info(
        "Generated briefing. briefingDate={}, sourceCount={}", briefingDate, articleIds.size());
  }

  private List<ListItemProjection> findMostViewedSources(LocalDate briefingDate) {
    Instant start = briefingDate.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant end = briefingDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    List<ListItemProjection> rows =
        articleRepository.findMostViewedByPublishedAtBetweenAndLicenseCodeIn(
            SupportedLanguage.ENGLISH.code(),
            start,
            end,
            licensePolicyEvaluator.getBriefingSourceLicenseCodes(),
            PageRequest.of(0, properties.articleCount() * SOURCE_FETCH_MULTIPLIER));
    return distinctById(rows).stream().limit(properties.articleCount()).toList();
  }

  private List<ListItemProjection> distinctById(List<ListItemProjection> rows) {
    return rows.stream()
        .collect(
            Collectors.toMap(
                ListItemProjection::id, row -> row, (first, ignored) -> first, LinkedHashMap::new))
        .values()
        .stream()
        .toList();
  }

  private BriefingGenerationClient.Request toRequest(List<ListItemProjection> sources) {
    List<BriefingGenerationClient.Request.SourceArticle> articles =
        sources.stream()
            .map(
                source ->
                    new BriefingGenerationClient.Request.SourceArticle(
                        source.title(), source.summary()))
            .toList();
    return new BriefingGenerationClient.Request(articles);
  }
}
