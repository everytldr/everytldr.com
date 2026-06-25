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
           WHERE comment.article = a))
      FROM Article a
        JOIN ArticleSummary s ON s.article = a AND s.language = :language
        JOIN ArticleCategory ac ON ac.article = a
        JOIN ac.category c
      WHERE a.id = :id
        AND a.licenseInfo.licenseCode IN :publishableLicenseCodes
      """)
  Optional<DetailProjection> findPublishableDetailByIdAndLanguage(
      @Param("id") Long id,
      @Param("language") String language,
      @Param("publishableLicenseCodes") Collection<LicenseCode> publishableLicenseCodes);

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
      WHERE a.licenseInfo.licenseCode IN :publishableLicenseCodes
        AND (:cursorPublishedAt IS NULL
          OR a.publishedAt < :cursorPublishedAt
          OR (a.publishedAt = :cursorPublishedAt AND a.id < :cursorId))
      ORDER BY a.publishedAt DESC, a.id DESC
      """)
  List<ListItemProjection> findRecentPublishable(
      @Param("language") String language,
      @Param("cursorPublishedAt") Instant cursorPublishedAt,
      @Param("cursorId") Long cursorId,
      @Param("publishableLicenseCodes") Collection<LicenseCode> publishableLicenseCodes,
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
      WHERE a.licenseInfo.licenseCode IN :publishableLicenseCodes
        AND (:cursorPublishedAt IS NULL
          OR a.publishedAt < :cursorPublishedAt
          OR (a.publishedAt = :cursorPublishedAt AND a.id < :cursorId))
        AND (c.slug = :categoryPrefix OR c.slug LIKE CONCAT(:categoryPrefix, '-%'))
      ORDER BY a.publishedAt DESC, a.id DESC
      """)
  List<ListItemProjection> findRecentPublishableByCategoryPrefix(
      @Param("language") String language,
      @Param("categoryPrefix") String categoryPrefix,
      @Param("cursorPublishedAt") Instant cursorPublishedAt,
      @Param("cursorId") Long cursorId,
      @Param("publishableLicenseCodes") Collection<LicenseCode> publishableLicenseCodes,
      Pageable pageable);

  @Query(
      """
      SELECT a
      FROM Article a
      WHERE a.id = :id
        AND a.licenseInfo.licenseCode IN :publishableLicenseCodes
      """)
  Optional<Article> findPublishableById(
      @Param("id") Long id,
      @Param("publishableLicenseCodes") Collection<LicenseCode> publishableLicenseCodes);

  @Query(
      """
      SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END
      FROM Article a
      WHERE a.id = :id
        AND a.licenseInfo.licenseCode IN :publishableLicenseCodes
      """)
  boolean existsPublishableById(
      @Param("id") Long id,
      @Param("publishableLicenseCodes") Collection<LicenseCode> publishableLicenseCodes);

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
      long commentCount) {
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
