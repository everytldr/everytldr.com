package com.everytldr.api.article;

import com.everytldr.api.support.pagination.Pagination;
import com.everytldr.common.domain.article.Article;
import com.everytldr.common.domain.article.ArticleRepository;
import com.everytldr.common.domain.article.ArticleRepository.DetailProjection;
import com.everytldr.common.domain.article.ArticleRepository.ListItemProjection;
import com.everytldr.common.domain.article.ArticleRepository.SearchItemProjection;
import com.everytldr.common.domain.language.SupportedLanguage;
import com.everytldr.common.domain.license.LicenseCode;
import com.everytldr.common.domain.license.LicensePolicyEvaluator;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Profile("api")
public class ArticleService {
  private final ArticleRepository articleRepository;
  private final LicensePolicyEvaluator licensePolicyEvaluator;

  public Article getArticleOrThrow(Long articleId) {
    return articleRepository
        .findByIdAndLicenseCodeIn(articleId, getPublishableLicenseCodes())
        .orElseThrow(() -> new ArticleExceptions.NotFound(articleId));
  }

  public void assertArticleExists(Long articleId) {
    if (!articleRepository.existsByIdAndLicenseCodeIn(articleId, getPublishableLicenseCodes())) {
      throw new ArticleExceptions.NotFound(articleId);
    }
  }

  public DetailProjection getArticleDetail(Long id, SupportedLanguage language) {
    return articleRepository
        .findDetailByIdAndLanguageAndLicenseCodeIn(
            id, language.code(), getPublishableLicenseCodes())
        .orElseThrow(() -> new ArticleExceptions.NotFound(id));
  }

  public Pagination.Page<ListItemProjection> listRecent(
      SupportedLanguage language,
      String categoryPrefix,
      Instant cursorPublishedAt,
      Long cursorId,
      int size) {
    PageRequest pageRequest =
        PageRequest.of(
            0,
            size + 1, // NOTE: 행 하나를 추가로 받아와서 hasMore를 계산해냄.
            Sort.unsorted());
    List<ListItemProjection> rows =
        categoryPrefix == null
            ? articleRepository.findRecentByLicenseCodeIn(
                language.code(),
                cursorPublishedAt,
                cursorId,
                getPublishableLicenseCodes(),
                pageRequest)
            : articleRepository.findRecentByCategoryPrefixAndLicenseCodeIn(
                language.code(),
                categoryPrefix,
                cursorPublishedAt,
                cursorId,
                getPublishableLicenseCodes(),
                pageRequest);
    return Pagination.Page.from(rows, size);
  }

  public SearchResult search(SupportedLanguage language, String query, int offset, int size) {
    String trimmedQuery = query.trim();
    List<String> publishableLicenseCodeValues =
        getPublishableLicenseCodes().stream().map(LicenseCode::value).toList();
    List<SearchItemProjection> rows =
        articleRepository.searchByLicenseCodeIn(
            trimmedQuery, language.code(), publishableLicenseCodeValues, size + 1, offset);

    boolean hasMore = rows.size() > size;
    List<ListItemProjection> items =
        (hasMore ? rows.subList(0, size) : rows)
            .stream().map(SearchItemProjection::toListItem).toList();
    Integer nextOffset = hasMore ? offset + size : null;
    return new SearchResult(items, nextOffset);
  }

  private Collection<LicenseCode> getPublishableLicenseCodes() {
    return licensePolicyEvaluator.getPublishableTransformedTextLicenseCodes();
  }

  public record SearchResult(List<ListItemProjection> items, Integer nextOffset) {}
}
