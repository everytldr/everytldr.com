package com.everytldr.common.domain.briefing;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BriefingRepository extends JpaRepository<Briefing, Long> {

  boolean existsByBriefingDate(LocalDate briefingDate);

  Optional<Briefing> findByBriefingDateAndLanguage(LocalDate briefingDate, String language);

  List<Briefing> findByLanguageOrderByBriefingDateDesc(String language, Pageable pageable);

  List<Briefing> findByLanguageAndBriefingDateLessThanOrderByBriefingDateDesc(
      String language, LocalDate cursor, Pageable pageable);
}
