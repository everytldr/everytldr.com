package com.everytldr.common.domain.category;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleCategoryRepository extends JpaRepository<ArticleCategory, Long> {
  List<ArticleCategory> findAllByArticleId(Long articleId);
}
