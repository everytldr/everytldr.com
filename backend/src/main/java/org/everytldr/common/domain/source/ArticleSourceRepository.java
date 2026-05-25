package org.everytldr.common.domain.source;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleSourceRepository extends JpaRepository<ArticleSource, Long> {
  List<ArticleSource> findAllByIsActiveTrue();
}
