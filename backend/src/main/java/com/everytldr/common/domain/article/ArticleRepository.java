package com.everytldr.common.domain.article;

import java.time.Instant;
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
          a.sourceUrl,
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
      """)
  Optional<DetailProjection> findDetailByIdAndLanguage(
      @Param("id") Long id, @Param("language") String language);

  @Query(
      """
      SELECT new com.everytldr.common.domain.article.ArticleRepository$ListItemProjection(
          a.id, s.title, s.content, a.thumbnailUrl, a.publishedAt, a.source, c.slug)
      FROM Article a
        JOIN ArticleSummary s ON s.article = a AND s.language = :language
        JOIN ArticleCategory ac ON ac.article = a
        JOIN ac.category c
      WHERE (:cursorPublishedAt IS NULL
          OR a.publishedAt < :cursorPublishedAt
          OR (a.publishedAt = :cursorPublishedAt AND a.id < :cursorId))
        AND (:categoryPrefix IS NULL OR c.slug LIKE CONCAT(:categoryPrefix, '%'))
      ORDER BY a.publishedAt DESC, a.id DESC
      """)
  List<ListItemProjection> findRecent(
      @Param("language") String language,
      @Param("categoryPrefix") String categoryPrefix,
      @Param("cursorPublishedAt") Instant cursorPublishedAt,
      @Param("cursorId") Long cursorId,
      Pageable pageable);

  record DetailProjection(
      Long id,
      String title,
      String summary,
      String thumbnailUrl,
      Instant publishedAt,
      String source,
      String sourceUrl,
      String category,
      long likeCount,
      long commentCount) {}

  record ListItemProjection(
      Long id,
      String title,
      String summary,
      String thumbnailUrl,
      Instant publishedAt,
      String source,
      String categorySlug) {}
}
