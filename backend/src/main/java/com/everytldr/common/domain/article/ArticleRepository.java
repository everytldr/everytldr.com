package com.everytldr.common.domain.article;

import com.everytldr.common.domain.license.LicenseCode;
import com.everytldr.common.domain.license.LicenseInfo;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleRepository extends JpaRepository<Article, Long> {

  @Query(
      """
      SELECT new com.everytldr.common.domain.article.ArticleRepository$DetailProjection(
          a.id,
          s.title,
          s.content,
          a.thumbnailUrl,
          a.publishedAt,
          a.source,
          a.contentUrl,
          a.licenseInfo.licenseCode,
          a.licenseInfo.licenseVersion,
          c.slug,
          (SELECT COUNT(l.id)
           FROM ArticleLike l
           WHERE l.article = a AND l.isActive = TRUE),
           (SELECT COUNT(comment.id)
            FROM ArticleComment comment
            WHERE comment.article = a),
           a.viewCount)
      FROM Article a
        JOIN ArticleSummary s ON s.article = a AND s.language = :language
        JOIN ArticleCategory ac ON ac.article = a
        JOIN ac.category c
      WHERE a.id = :id
        AND a.licenseInfo.licenseCode IN :licenseCodes
      """)
  Optional<DetailProjection> findDetailByIdAndLanguageAndLicenseCodeIn(
      @Param("id") Long id,
      @Param("language") String language,
      @Param("licenseCodes") Collection<LicenseCode> licenseCodes);

  @Query(
      """
      SELECT new com.everytldr.common.domain.article.ArticleRepository$ListItemProjection(
          a.id,
          s.title,
          s.content,
          a.thumbnailUrl,
          a.publishedAt,
          a.source,
          a.licenseInfo.licenseCode,
          a.licenseInfo.licenseVersion,
          c.slug)
      FROM Article a
        JOIN ArticleSummary s ON s.article = a AND s.language = :language
        JOIN ArticleCategory ac ON ac.article = a
        JOIN ac.category c
      WHERE a.licenseInfo.licenseCode IN :licenseCodes
        AND (:cursorPublishedAt IS NULL
          OR a.publishedAt < :cursorPublishedAt
          OR (a.publishedAt = :cursorPublishedAt AND a.id < :cursorId))
      ORDER BY a.publishedAt DESC, a.id DESC
      """)
  List<ListItemProjection> findRecentByLicenseCodeIn(
      @Param("language") String language,
      @Param("cursorPublishedAt") Instant cursorPublishedAt,
      @Param("cursorId") Long cursorId,
      @Param("licenseCodes") Collection<LicenseCode> licenseCodes,
      Pageable pageable);

  @Query(
      """
      SELECT new com.everytldr.common.domain.article.ArticleRepository$ListItemProjection(
          a.id,
          s.title,
          s.content,
          a.thumbnailUrl,
          a.publishedAt,
          a.source,
          a.licenseInfo.licenseCode,
          a.licenseInfo.licenseVersion,
          c.slug)
      FROM Article a
        JOIN ArticleSummary s ON s.article = a AND s.language = :language
        JOIN ArticleCategory ac ON ac.article = a
        JOIN ac.category c
      WHERE a.licenseInfo.licenseCode IN :licenseCodes
        AND (:cursorPublishedAt IS NULL
          OR a.publishedAt < :cursorPublishedAt
          OR (a.publishedAt = :cursorPublishedAt AND a.id < :cursorId))
        AND (c.slug = :categoryPrefix OR c.slug LIKE CONCAT(:categoryPrefix, '-%'))
      ORDER BY a.publishedAt DESC, a.id DESC
      """)
  List<ListItemProjection> findRecentByCategoryPrefixAndLicenseCodeIn(
      @Param("language") String language,
      @Param("categoryPrefix") String categoryPrefix,
      @Param("cursorPublishedAt") Instant cursorPublishedAt,
      @Param("cursorId") Long cursorId,
      @Param("licenseCodes") Collection<LicenseCode> licenseCodes,
      Pageable pageable);

  @Query(
      """
      SELECT new com.everytldr.common.domain.article.ArticleRepository$ListItemProjection(
          a.id,
          s.title,
          s.content,
          a.thumbnailUrl,
          a.publishedAt,
          a.source,
          a.licenseInfo.licenseCode,
          a.licenseInfo.licenseVersion,
          c.slug)
      FROM Article a
        JOIN ArticleSummary s ON s.article = a AND s.language = :language
        JOIN ArticleCategory ac ON ac.article = a
        JOIN ac.category c
      WHERE a.id IN :articleIds
        AND a.licenseInfo.licenseCode IN :licenseCodes
      """)
  List<ListItemProjection> findListItemsByIdInAndLanguageAndLicenseCodeIn(
      @Param("articleIds") Collection<Long> articleIds,
      @Param("language") String language,
      @Param("licenseCodes") Collection<LicenseCode> licenseCodes);

  @Query(
      """
      SELECT new com.everytldr.common.domain.article.ArticleRepository$ListItemProjection(
          a.id,
          s.title,
          s.content,
          a.thumbnailUrl,
          a.publishedAt,
          a.source,
          a.licenseInfo.licenseCode,
          a.licenseInfo.licenseVersion,
          c.slug)
      FROM Article a
        JOIN ArticleSummary s ON s.article = a AND s.language = :language
        JOIN ArticleCategory ac ON ac.article = a
        JOIN ac.category c
      WHERE a.licenseInfo.licenseCode IN :licenseCodes
      ORDER BY a.viewCount DESC, a.publishedAt DESC, a.id DESC
      """)
  List<ListItemProjection> findMostViewedByLanguageAndLicenseCodeIn(
      @Param("language") String language,
      @Param("licenseCodes") Collection<LicenseCode> licenseCodes,
      Pageable pageable);

  @Query(
      """
      SELECT a
      FROM Article a
      WHERE a.id = :id
        AND a.licenseInfo.licenseCode IN :licenseCodes
      """)
  Optional<Article> findByIdAndLicenseCodeIn(
      @Param("id") Long id, @Param("licenseCodes") Collection<LicenseCode> licenseCodes);

  @Query(
      """
      SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END
      FROM Article a
      WHERE a.id = :id
        AND a.licenseInfo.licenseCode IN :licenseCodes
      """)
  boolean existsByIdAndLicenseCodeIn(
      @Param("id") Long id, @Param("licenseCodes") Collection<LicenseCode> licenseCodes);

  @Query(
      """
      SELECT new com.everytldr.common.domain.article.ArticleRepository$SitemapItemProjection(
          a.id,
          a.publishedAt)
      FROM Article a
      WHERE a.licenseInfo.licenseCode IN :licenseCodes
        AND EXISTS (SELECT 1 FROM ArticleCategory ac WHERE ac.article = a)
        AND EXISTS (SELECT 1 FROM ArticleSummary s WHERE s.article = a)
      ORDER BY a.publishedAt ASC, a.id ASC
      """)
  List<SitemapItemProjection> findAllForSitemapByLicenseCodeIn(
      @Param("licenseCodes") Collection<LicenseCode> licenseCodes, Pageable pageable);

  @Query(
      """
      SELECT COUNT(a)
      FROM Article a
      WHERE a.licenseInfo.licenseCode IN :licenseCodes
        AND EXISTS (SELECT 1 FROM ArticleCategory ac WHERE ac.article = a)
        AND EXISTS (SELECT 1 FROM ArticleSummary s WHERE s.article = a)
      """)
  long countAllForSitemapByLicenseCodeIn(
      @Param("licenseCodes") Collection<LicenseCode> licenseCodes);

  @Query(
      """
      SELECT new com.everytldr.common.domain.article.ArticleRepository$SitemapLanguageProjection(
          s.article.id,
          s.language)
      FROM ArticleSummary s
      WHERE s.article.id IN :articleIds
      """)
  List<SitemapLanguageProjection> findSitemapLanguagesByArticleIdIn(
      @Param("articleIds") Collection<Long> articleIds);

  @Query(
      """
      SELECT new com.everytldr.common.domain.article.ArticleRepository$NewsSitemapItemProjection(
          a.id,
          a.publishedAt,
          s.language,
          s.title)
      FROM Article a
        JOIN ArticleSummary s ON s.article = a
      WHERE a.licenseInfo.licenseCode IN :licenseCodes
        AND a.publishedAt >= :publishedAfter
        AND EXISTS (SELECT 1 FROM ArticleCategory ac WHERE ac.article = a)
      ORDER BY a.publishedAt DESC, a.id DESC
      """)
  List<NewsSitemapItemProjection> findRecentForNewsSitemapByLicenseCodeIn(
      @Param("licenseCodes") Collection<LicenseCode> licenseCodes,
      @Param("publishedAfter") Instant publishedAfter,
      Pageable pageable);

  @Query(
      value =
          """
          SELECT a.id AS id,
                 s.title AS title,
                 s.content AS summary,
                 a.thumbnail_url AS thumbnailUrl,
                 a.published_at AS publishedAt,
                 a.source AS source,
                 a.license_code AS licenseCode,
                 a.license_version AS licenseVersion,
                 c.slug AS categorySlug
          FROM article a
            JOIN article_summary s ON s.article_id = a.id AND s.language = :language
            JOIN article_category ac ON ac.article_id = a.id
            JOIN category c ON c.id = ac.category_id
          WHERE a.deleted_at IS NULL
            AND a.license_code IN (:licenseCodes)
            AND MATCH(s.title, s.content) AGAINST (:query IN NATURAL LANGUAGE MODE)
          ORDER BY MATCH(s.title, s.content) AGAINST (:query IN NATURAL LANGUAGE MODE) DESC,
                   a.published_at DESC,
                   a.id DESC
          LIMIT :limit OFFSET :offset
          """,
      nativeQuery = true)
  List<SearchItemProjection> searchByLicenseCodeIn(
      @Param("query") String query,
      @Param("language") String language,
      @Param("licenseCodes") Collection<String> licenseCodes,
      @Param("limit") int limit,
      @Param("offset") int offset);

  @Query(
      """
      SELECT new com.everytldr.common.domain.article.ArticleRepository$RelatedSeedProjection(
          s.title,
          c.slug)
      FROM Article a
        JOIN ArticleSummary s ON s.article = a AND s.language = :language
        JOIN ArticleCategory ac ON ac.article = a
        JOIN ac.category c
      WHERE a.id = :id
        AND a.licenseInfo.licenseCode IN :licenseCodes
      """)
  Optional<RelatedSeedProjection> findRelatedSeedByIdAndLanguageAndLicenseCodeIn(
      @Param("id") Long id,
      @Param("language") String language,
      @Param("licenseCodes") Collection<LicenseCode> licenseCodes);

  @Query(
      value =
          """
          SELECT a.id AS id,
                 s.title AS title,
                 s.content AS summary,
                 a.thumbnail_url AS thumbnailUrl,
                 a.published_at AS publishedAt,
                 a.source AS source,
                 a.license_code AS licenseCode,
                 a.license_version AS licenseVersion,
                 c.slug AS categorySlug
          FROM article a
            JOIN article_summary s ON s.article_id = a.id AND s.language = :language
            JOIN article_category ac ON ac.article_id = a.id
            JOIN category c ON c.id = ac.category_id
          WHERE a.deleted_at IS NULL
            AND a.id <> :articleId
            AND a.license_code IN (:licenseCodes)
            AND MATCH(s.title, s.content) AGAINST (:query IN NATURAL LANGUAGE MODE)
          ORDER BY MATCH(s.title, s.content) AGAINST (:query IN NATURAL LANGUAGE MODE)
                     * (CASE WHEN c.slug = :categorySlug THEN :categoryBoost ELSE 1 END) DESC,
                   a.published_at DESC,
                   a.id DESC
          LIMIT :limit
          """,
      nativeQuery = true)
  List<SearchItemProjection> findRelatedByLicenseCodeIn(
      @Param("articleId") Long articleId,
      @Param("query") String query,
      @Param("categorySlug") String categorySlug,
      @Param("categoryBoost") double categoryBoost,
      @Param("language") String language,
      @Param("licenseCodes") Collection<String> licenseCodes,
      @Param("limit") int limit);

  interface SearchItemProjection {
    Long getId();

    String getTitle();

    String getSummary();

    String getThumbnailUrl();

    Instant getPublishedAt();

    String getSource();

    String getLicenseCode();

    String getLicenseVersion();

    String getCategorySlug();

    default ListItemProjection toListItem() {
      return new ListItemProjection(
          getId(),
          getTitle(),
          getSummary(),
          getThumbnailUrl(),
          getPublishedAt(),
          getSource(),
          LicenseCode.fromValue(getLicenseCode()),
          getLicenseVersion(),
          getCategorySlug());
    }
  }

  record RelatedSeedProjection(String title, String categorySlug) {}

  record SitemapItemProjection(Long id, Instant publishedAt) {}

  record SitemapLanguageProjection(Long articleId, String language) {}

  record NewsSitemapItemProjection(Long id, Instant publishedAt, String language, String title) {}

  record DetailProjection(
      Long id,
      String title,
      String summary,
      String thumbnailUrl,
      Instant publishedAt,
      String source,
      String contentUrl,
      LicenseCode licenseCode,
      String licenseVersion,
      String category,
      long likeCount,
      long commentCount,
      long viewCount) {
    public String licenseCodeValue() {
      return licenseCode.value();
    }

    public LicenseInfo licenseInfo() {
      return new LicenseInfo(licenseCode, licenseVersion);
    }
  }

  record ListItemProjection(
      Long id,
      String title,
      String summary,
      String thumbnailUrl,
      Instant publishedAt,
      String source,
      LicenseCode licenseCode,
      String licenseVersion,
      String categorySlug) {
    public String licenseCodeValue() {
      return licenseCode.value();
    }

    public LicenseInfo licenseInfo() {
      return new LicenseInfo(licenseCode, licenseVersion);
    }
  }
}
