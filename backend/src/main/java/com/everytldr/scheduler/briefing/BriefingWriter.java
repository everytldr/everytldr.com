package com.everytldr.scheduler.briefing;

import com.everytldr.common.domain.article.ArticleRepository;
import com.everytldr.common.domain.briefing.Briefing;
import com.everytldr.common.domain.briefing.BriefingArticle;
import com.everytldr.common.domain.briefing.BriefingArticleRepository;
import com.everytldr.common.domain.briefing.BriefingRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Profile("scheduler")
public class BriefingWriter {
  private final BriefingRepository briefingRepository;
  private final BriefingArticleRepository briefingArticleRepository;
  private final ArticleRepository articleRepository;

  @Transactional
  public void save(
      LocalDate briefingDate,
      List<BriefingGenerationClient.Result> results,
      List<Long> articleIds) {
    for (BriefingGenerationClient.Result result : results) {
      briefingRepository.save(
          Briefing.create(briefingDate, result.language(), result.title(), result.content()));
    }
    for (Long articleId : articleIds) {
      briefingArticleRepository.save(
          BriefingArticle.create(briefingDate, articleRepository.getReferenceById(articleId)));
    }
  }
}
