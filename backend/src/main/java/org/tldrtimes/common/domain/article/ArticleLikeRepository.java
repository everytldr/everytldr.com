package org.tldrtimes.common.domain.article;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleLikeRepository extends JpaRepository<ArticleLike, Long> {
  Optional<ArticleLike> findByArticleIdAndIpHash(Long articleId, String ipHash);

  long countByArticleIdAndIsActiveTrue(Long articleId);
}
