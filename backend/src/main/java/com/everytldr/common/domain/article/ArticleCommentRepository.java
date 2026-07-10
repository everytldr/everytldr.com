package com.everytldr.common.domain.article;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleCommentRepository extends JpaRepository<ArticleComment, Long> {
  @Query(
      value =
          """
          SELECT c.* FROM article_comment c
          WHERE c.article_id = :articleId
            AND (c.deleted_at IS NULL
                 OR EXISTS (SELECT 1 FROM article_comment r
                            WHERE r.parent_id = c.id AND r.deleted_at IS NULL))
          ORDER BY c.id ASC
          """,
      nativeQuery = true)
  List<ArticleComment> findThreadByArticleId(@Param("articleId") Long articleId);

  Optional<ArticleComment> findByIdAndArticleId(Long id, Long articleId);
}
