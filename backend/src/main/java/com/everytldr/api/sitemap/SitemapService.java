package com.everytldr.api.sitemap;

import com.everytldr.common.domain.article.ArticleRepository;
import com.everytldr.common.domain.article.ArticleRepository.SitemapItemProjection;
import com.everytldr.common.domain.article.ArticleRepository.SitemapLanguageProjection;
import com.everytldr.common.domain.license.LicenseCode;
import com.everytldr.common.domain.license.LicensePolicyEvaluator;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
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
  private final ArticleRepository articleRepository;
  private final LicensePolicyEvaluator licensePolicyEvaluator;

  public long countArticles() {
    return articleRepository.countAllForSitemapByLicenseCodeIn(getPublishableLicenseCodes());
  }

  public List<SitemapArticle> findArticles(int page, int size) {
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
}
