package com.everytldr.api.briefing;

import com.everytldr.api.support.pagination.Pagination;
import com.everytldr.common.domain.briefing.Briefing;
import com.everytldr.common.domain.briefing.BriefingArticleRepository;
import com.everytldr.common.domain.briefing.BriefingRepository;
import com.everytldr.common.domain.language.SupportedLanguage;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Profile("api")
public class BriefingService {
  private final BriefingRepository briefingRepository;
  private final BriefingArticleRepository briefingArticleRepository;

  public ListResult listRecent(SupportedLanguage language, LocalDate cursor, int size) {
    Objects.requireNonNull(language, "language must not be null");

    PageRequest pageRequest = PageRequest.of(0, size + 1);
    List<Briefing> rows =
        cursor == null
            ? briefingRepository.findByLanguageOrderByBriefingDateDesc(language.code(), pageRequest)
            : briefingRepository.findByLanguageAndBriefingDateLessThanOrderByBriefingDateDesc(
                language.code(), cursor, pageRequest);

    Pagination.Page<Briefing> page = Pagination.Page.from(rows, size);
    LocalDate nextCursor = page.nextStart() == null ? null : page.nextStart().getBriefingDate();
    return new ListResult(page.items(), nextCursor);
  }

  public Briefing getBriefing(SupportedLanguage language, LocalDate briefingDate) {
    Objects.requireNonNull(language, "language must not be null");
    Objects.requireNonNull(briefingDate, "briefingDate must not be null");

    return briefingRepository
        .findByBriefingDateAndLanguage(briefingDate, language.code())
        .orElseThrow(() -> new BriefingExceptions.NotFound(briefingDate));
  }

  public Optional<Briefing> findBriefingForArticle(SupportedLanguage language, Long articleId) {
    Objects.requireNonNull(language, "language must not be null");
    Objects.requireNonNull(articleId, "articleId must not be null");

    return briefingRepository
        .findByArticleIdAndLanguageOrderByBriefingDateDesc(
            articleId, language.code(), PageRequest.of(0, 1))
        .stream()
        .findFirst();
  }

  public List<Long> listArticleIds(LocalDate briefingDate) {
    Objects.requireNonNull(briefingDate, "briefingDate must not be null");

    return briefingArticleRepository.findArticleIdsByBriefingDate(briefingDate);
  }

  public record ListResult(List<Briefing> items, LocalDate nextCursor) {}
}
