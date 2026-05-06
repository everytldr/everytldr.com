package org.tldrtimes.common.domain.ingestion;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleIngestionJobRepository extends JpaRepository<ArticleIngestionJob, Long> {
  Optional<ArticleIngestionJob> findByUrlHash(byte[] urlHash);

  Optional<ArticleIngestionJob> findByArticleId(Long articleId);

  boolean existsByUrlHash(byte[] urlHash);

  @Query("SELECT j.urlHash FROM ArticleIngestionJob j WHERE j.urlHash IN :urlHashes")
  List<byte[]> findExistingUrlHashes(@Param("urlHashes") Collection<byte[]> urlHashes);
}
