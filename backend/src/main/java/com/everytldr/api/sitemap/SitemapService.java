package com.everytldr.api.sitemap;

import com.everytldr.common.domain.article.ArticleRepository;
import com.everytldr.common.domain.article.ArticleRepository.NewsSitemapItemProjection;
import com.everytldr.common.domain.article.ArticleRepository.SitemapItemProjection;
import com.everytldr.common.domain.article.ArticleRepository.SitemapLanguageProjection;
import com.everytldr.common.domain.language.SupportedLanguage;
import com.everytldr.common.domain.license.LicenseCode;
import com.everytldr.common.domain.license.LicensePolicyEvaluator;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@Profile("api")
@RequiredArgsConstructor
public class SitemapService {
  private static final int MAX_SUMMARIES_PER_ARTICLE = SupportedLanguage.values().length;

  private final ArticleRepository articleRepository;
  private final LicensePolicyEvaluator licensePolicyEvaluator;

  public long countSitemapArticles() {
    return articleRepository.countAllForSitemapByLicenseCodeIn(getPublishableLicenseCodes());
  }

  public List<SitemapArticle> findSitemapArticles(int page, int size) {
    List<SitemapItemProjection> items =
        articleRepository.findAllForSitemapByLicenseCodeIn(
            getPublishableLicenseCodes(), PageRequest.of(page, size));
    if (items.isEmpty()) {
      return List.of();
    }

    Map<Long, List<String>> languagesByArticleId = findLanguagesByArticleId(items);

    return items.stream()
        .map(
            item ->
                new SitemapArticle(
                    item.id(),
                    item.publishedAt(),
                    languagesByArticleId.getOrDefault(item.id(), List.of())))
        .toList();
  }

  public List<NewsSitemapArticle> findNewsSitemapArticles(Duration window, int maxArticles) {
    Instant publishedAfter = Instant.now().minus(window);
    List<NewsSitemapItemProjection> rows =
        articleRepository.findRecentForNewsSitemapByLicenseCodeIn(
            getPublishableLicenseCodes(),
            publishedAfter,
            PageRequest.of(0, maxArticles * MAX_SUMMARIES_PER_ARTICLE));

    Map<Long, List<NewsSitemapItemProjection>> rowsByArticleId =
        rows.stream()
            .collect(
                Collectors.groupingBy(
                    NewsSitemapItemProjection::id, LinkedHashMap::new, Collectors.toList()));

    return rowsByArticleId.values().stream()
        .limit(maxArticles)
        .map(SitemapService::toNewsSitemapArticle)
        .toList();
  }

  private static NewsSitemapArticle toNewsSitemapArticle(List<NewsSitemapItemProjection> rows) {
    NewsSitemapItemProjection first = rows.getFirst();
    List<NewsSitemapSummary> summaries =
        rows.stream()
            .map(row -> new NewsSitemapSummary(row.language(), row.title()))
            .sorted(Comparator.comparing(NewsSitemapSummary::language))
            .toList();

    return new NewsSitemapArticle(first.id(), first.publishedAt(), summaries);
  }

  private Map<Long, List<String>> findLanguagesByArticleId(List<SitemapItemProjection> items) {
    List<Long> articleIds = items.stream().map(SitemapItemProjection::id).toList();

    return articleRepository.findSitemapLanguagesByArticleIdIn(articleIds).stream()
        .collect(
            Collectors.groupingBy(
                SitemapLanguageProjection::articleId,
                Collectors.mapping(
                    SitemapLanguageProjection::language,
                    Collectors.collectingAndThen(
                        Collectors.toList(),
                        languages ->
                            languages.stream().sorted(Comparator.naturalOrder()).toList()))));
  }

  private Collection<LicenseCode> getPublishableLicenseCodes() {
    return licensePolicyEvaluator.getPublishableTransformedTextLicenseCodes();
  }

  public record SitemapArticle(Long id, Instant publishedAt, List<String> languages) {}

  public record NewsSitemapArticle(
      Long id, Instant publishedAt, List<NewsSitemapSummary> summaries) {}

  public record NewsSitemapSummary(String language, String title) {}
}
