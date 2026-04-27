package org.tldrtimes.common.domain.ingestion;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleIngestionJobRepository extends JpaRepository<ArticleIngestionJob, Long> {

    Optional<ArticleIngestionJob> findByUrlHash(byte[] urlHash);

    Optional<ArticleIngestionJob> findByArticleId(Long articleId);

    boolean existsByUrlHash(byte[] urlHash);
}
