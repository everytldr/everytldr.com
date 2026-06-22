package com.everytldr.enricher.content;

import static org.assertj.core.api.Assertions.assertThat;

import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.SourcePolicy;
import com.everytldr.common.domain.source.SourcePolicy.CrawlingPolicy;
import com.everytldr.common.domain.source.SourcePolicy.EligibilityPolicy;
import com.everytldr.common.domain.source.SourcePolicy.ThumbnailCandidateSelector;
import com.everytldr.common.domain.source.SourcePolicy.ThumbnailEligibilityPolicy;
import com.everytldr.common.domain.source.SourcePolicy.ThumbnailPolicy;
import com.everytldr.common.domain.source.SourceType;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

class ThumbnailEligibilityCheckerTest {
  private final ThumbnailEligibilityChecker checker = new ThumbnailEligibilityChecker();

  @Test
  void findsUnverifiedThumbnailWhenPolicyAllowsThumbnails() {
    Document document =
        document(
            """
            <html>
              <head><meta property="og:image" content="https://cdn.example.com/og.jpg" /></head>
              <body>
                <figure class="hero"><img src="https://cdn.example.com/hero.jpg" /></figure>
              </body>
            </html>
            """);

    assertThat(checker.findAllowedThumbnailUrl(document, source(ThumbnailPolicy.ALLOW), null))
        .contains("https://cdn.example.com/hero.jpg");
  }

  @Test
  void skipsThumbnailWhenCurrentThumbnailExists() {
    Document document =
        document(
            """
            <html>
              <head><meta property="og:image" content="https://cdn.example.com/og.jpg" /></head>
              <body></body>
            </html>
            """);

    assertThat(
            checker.findAllowedThumbnailUrl(
                document, source(ThumbnailPolicy.ALLOW), "https://cdn.example.com/current.jpg"))
        .isEmpty();
  }

  @Test
  void skipsThumbnailWhenPolicyDisablesThumbnails() {
    Document document =
        document(
            """
            <html>
              <head><meta property="og:image" content="https://cdn.example.com/og.jpg" /></head>
              <body></body>
            </html>
            """);

    assertThat(checker.findAllowedThumbnailUrl(document, source(ThumbnailPolicy.DISABLED), null))
        .isEmpty();
  }

  @Test
  void findsCreditVerifiedThumbnailWhenPolicyRequiresEligibility() {
    Document document =
        document(
            """
            <html>
              <body>
                <figure>
                  <img src="/images/voa.jpg" />
                  <figcaption>Photo: Voice of America</figcaption>
                </figure>
              </body>
            </html>
            """);

    assertThat(
            checker.findAllowedThumbnailUrl(document, source(ThumbnailPolicy.ELIGIBLE_ONLY), null))
        .contains("https://news.example.com/images/voa.jpg");
  }

  @Test
  void skipsCreditVerifiedThumbnailWhenCreditIsDenied() {
    Document document =
        document(
            """
            <html>
              <body>
                <figure>
                  <img src="/images/voa.jpg" />
                  <figcaption>Photo: Reuters</figcaption>
                </figure>
              </body>
            </html>
            """);

    assertThat(
            checker.findAllowedThumbnailUrl(document, source(ThumbnailPolicy.ELIGIBLE_ONLY), null))
        .isEmpty();
  }

  private ArticleSource source(ThumbnailPolicy thumbnailPolicy) {
    return ArticleSource.create(
        "Example News",
        new SourcePolicy(
            new CrawlingPolicy(
                List.of("https://news.example.com/feed/"),
                List.of("news.example.com"),
                List.of("article"),
                List.of(".hero img")),
            eligibilityPolicy(thumbnailPolicy)),
        "en",
        SourceType.RSS);
  }

  private EligibilityPolicy eligibilityPolicy(ThumbnailPolicy thumbnailPolicy) {
    if (thumbnailPolicy != ThumbnailPolicy.ELIGIBLE_ONLY) {
      return new EligibilityPolicy(List.of(), thumbnailPolicy, null);
    }

    return new EligibilityPolicy(
        List.of(),
        ThumbnailPolicy.ELIGIBLE_ONLY,
        new ThumbnailEligibilityPolicy(
            List.of(
                new ThumbnailCandidateSelector(
                    "figure img", "src", "figure", List.of("figcaption", ".caption"))),
            List.of("Voice of America", "VOA"),
            List.of("Reuters", "Associated Press", "Getty")));
  }

  private Document document(String html) {
    return Jsoup.parse(html, "https://news.example.com/story");
  }
}
