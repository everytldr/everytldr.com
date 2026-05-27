package org.everytldr.common.domain.article;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleCommentRepository extends JpaRepository<ArticleComment, Long> {
  List<ArticleComment> findByArticleIdOrderByIdAsc(Long articleId);

  Optional<ArticleComment> findByIdAndArticleId(Long id, Long articleId);
}
