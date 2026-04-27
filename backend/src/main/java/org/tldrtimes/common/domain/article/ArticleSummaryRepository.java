package org.tldrtimes.common.domain.article;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleSummaryRepository extends JpaRepository<ArticleSummary, Long> {

    Optional<ArticleSummary> findByArticleIdAndLanguage(Long articleId, String language);
}
