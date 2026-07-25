package com.everytldr.common.domain.briefing;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BriefingRepository extends JpaRepository<Briefing, Long> {

  boolean existsByBriefingDate(LocalDate briefingDate);

  Optional<Briefing> findByBriefingDateAndLanguage(LocalDate briefingDate, String language);

  List<Briefing> findByLanguageOrderByBriefingDateDesc(String language, Pageable pageable);

  List<Briefing> findByLanguageAndBriefingDateLessThanOrderByBriefingDateDesc(
      String language, LocalDate cursor, Pageable pageable);

  Optional<Briefing> findFirstByLanguageAndBriefingDateLessThanOrderByBriefingDateDesc(
      String language, LocalDate briefingDate);

  Optional<Briefing> findFirstByLanguageAndBriefingDateGreaterThanOrderByBriefingDateAsc(
      String language, LocalDate briefingDate);

  @Query(
      """
      SELECT b
      FROM Briefing b
      WHERE b.language = :language
        AND b.briefingDate IN (
          SELECT ba.briefingDate FROM BriefingArticle ba WHERE ba.article.id = :articleId
        )
      ORDER BY b.briefingDate DESC
      """)
  List<Briefing> findByArticleIdAndLanguageOrderByBriefingDateDesc(
      @Param("articleId") Long articleId, @Param("language") String language, Pageable pageable);
}
