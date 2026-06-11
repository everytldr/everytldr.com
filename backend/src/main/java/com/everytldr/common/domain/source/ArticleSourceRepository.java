package com.everytldr.common.domain.source;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleSourceRepository extends JpaRepository<ArticleSource, Long> {
  List<ArticleSource> findAllByIsActiveTrue();

  Optional<ArticleSource> findByName(String name);
}
