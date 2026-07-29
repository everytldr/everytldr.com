package com.everytldr.common.domain.briefing;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BriefingArticleRepository extends JpaRepository<BriefingArticle, Long> {

  @Query(
      """
      SELECT ba.article.id
      FROM BriefingArticle ba
      WHERE ba.briefingDate = :briefingDate
      ORDER BY ba.id ASC
      """)
  List<Long> findArticleIdsByBriefingDate(@Param("briefingDate") LocalDate briefingDate);
}
