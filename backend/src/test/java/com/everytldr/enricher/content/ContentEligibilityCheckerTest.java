package com.everytldr.enricher.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.SourcePolicy;
import com.everytldr.common.domain.source.SourcePolicy.ArticleEligibilityRule;
import com.everytldr.common.domain.source.SourcePolicy.CrawlingPolicy;
import com.everytldr.common.domain.source.SourcePolicy.EligibilityPolicy;
import com.everytldr.common.domain.source.SourcePolicy.RuleType;
import com.everytldr.common.domain.source.SourcePolicy.ThumbnailPolicy;
import com.everytldr.common.domain.source.SourceType;
import com.everytldr.enricher.enrichment.EnrichmentException;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

class ContentEligibilityCheckerTest {
  private final ContentEligibilityChecker checker = new ContentEligibilityChecker();

  @Test
  void allowsWhenSelectorTextContainsRequiredValue() {
    ArticleSource source =
        source(
            new EligibilityPolicy(
                List.of(
                    new ArticleEligibilityRule(
                        RuleType.SELECTOR_TEXT_CONTAINS_ANY,
                        ".license",
                        null,
                        List.of("Creative Commons"))),
                ThumbnailPolicy.ALLOW,
                null));

    Document document =
        Jsoup.parse(
            "<html><body><p class=\"license\">Creative Commons Attribution</p></body></html>");

    assertThatCode(() -> checker.assertEligible(document, source)).doesNotThrowAnyException();
  }

  @Test
  void rejectsWhenSelectorTextDoesNotContainRequiredValue() {
    ArticleSource source =
        source(
            new EligibilityPolicy(
                List.of(
                    new ArticleEligibilityRule(
                        RuleType.SELECTOR_TEXT_CONTAINS_ANY,
                        ".license",
                        null,
                        List.of("Creative Commons"))),
                ThumbnailPolicy.ALLOW,
                null));

    Document document =
        Jsoup.parse("<html><body><p class=\"license\">All rights reserved</p></body></html>");

    EnrichmentException exception =
        catchThrowableOfType(
            () -> checker.assertEligible(document, source), EnrichmentException.class);

    assertThat(exception).hasMessageContaining("selector text is missing required value");
    assertThat(exception.isRetryable()).isFalse();
  }

  @Test
  void allowsWhenDocumentHtmlContainsRequiredValue() {
    ArticleSource source =
        source(
            new EligibilityPolicy(
                List.of(
                    new ArticleEligibilityRule(
                        RuleType.DOCUMENT_HTML_CONTAINS_ANY,
                        null,
                        null,
                        List.of("Creative Commons"))),
                ThumbnailPolicy.ALLOW,
                null));

    Document document =
        Jsoup.parse("<html><body><p>Licensed under Creative Commons.</p></body></html>");

    assertThatCode(() -> checker.assertEligible(document, source)).doesNotThrowAnyException();
  }

  @Test
  void rejectsWhenDocumentHtmlDoesNotContainRequiredValue() {
    ArticleSource source =
        source(
            new EligibilityPolicy(
                List.of(
                    new ArticleEligibilityRule(
                        RuleType.DOCUMENT_HTML_CONTAINS_ANY,
                        null,
                        null,
                        List.of("Creative Commons"))),
                ThumbnailPolicy.ALLOW,
                null));

    Document document = Jsoup.parse("<html><body><p>All rights reserved.</p></body></html>");

    EnrichmentException exception =
        catchThrowableOfType(
            () -> checker.assertEligible(document, source), EnrichmentException.class);

    assertThat(exception).hasMessageContaining("document html is missing required value");
    assertThat(exception.isRetryable()).isFalse();
  }

  private ArticleSource source(EligibilityPolicy eligibilityPolicy) {
    return ArticleSource.create(
        "Example News",
        new SourcePolicy(
            new CrawlingPolicy(
                List.of("https://example.com/feed/"),
                List.of("example.com"),
                List.of("article"),
                List.of()),
            eligibilityPolicy),
        "en",
        SourceType.RSS);
  }
}
