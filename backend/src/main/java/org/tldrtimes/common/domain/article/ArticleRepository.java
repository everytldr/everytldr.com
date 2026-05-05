package org.tldrtimes.common.domain.article;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleRepository extends JpaRepository<Article, Long> {
  @Query(
      """
      SELECT new org.tldrtimes.common.domain.article.ArticleListProjection(
          a.id, s.title, s.content, a.thumbnailUrl, a.publishedAt, a.source, c.slug)
      FROM Article a
        JOIN ArticleSummary s ON s.article = a AND s.language = :language
        JOIN ArticleCategory ac ON ac.article = a
        JOIN ac.category c
      WHERE (:cursorPublishedAt IS NULL
          OR a.publishedAt < :cursorPublishedAt
          OR (a.publishedAt = :cursorPublishedAt AND a.id < :cursorId))
        AND (:categorySlug IS NULL OR c.slug = :categorySlug)
      ORDER BY a.publishedAt DESC, a.id DESC
      """)
  List<ArticleListProjection> findRecent(
      @Param("language") String language,
      @Param("categorySlug") String categorySlug,
      @Param("cursorPublishedAt") Instant cursorPublishedAt,
      @Param("cursorId") Long cursorId,
      Pageable pageable);
}
