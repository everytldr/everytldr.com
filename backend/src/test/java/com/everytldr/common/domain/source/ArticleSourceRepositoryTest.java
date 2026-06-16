package com.everytldr.common.domain.source;

import static org.assertj.core.api.Assertions.assertThat;

import com.everytldr.TestcontainersConfig;
import com.everytldr.common.domain.source.SourcePolicy.ArticleEligibilityRule;
import com.everytldr.common.domain.source.SourcePolicy.EligibilityPolicy;
import com.everytldr.common.domain.source.SourcePolicy.RuleType;
import com.everytldr.common.domain.source.SourcePolicy.ThumbnailPolicy;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
@Transactional
class ArticleSourceRepositoryTest {

  @Autowired private ArticleSourceRepository articleSourceRepository;

  @Test
  void findsSeededActiveGlobalVoicesRssSource() {
    List<ArticleSource> activeSources = articleSourceRepository.findAllByIsActiveTrue();

    ArticleSource globalVoicesSource =
        activeSources.stream()
            .filter(source -> "Global Voices".equals(source.getName()))
            .findFirst()
            .orElseThrow();

    assertThat(globalVoicesSource.getId()).isEqualTo(45660871069790211L);
    assertThat(globalVoicesSource.getName()).isEqualTo("Global Voices");
    assertThat(globalVoicesSource.getLanguage()).isEqualTo("en");
    assertThat(globalVoicesSource.getSourceType()).isEqualTo(SourceType.RSS);
    assertThat(globalVoicesSource.getPolicy().crawling().feedUrls())
        .hasSize(47)
        .startsWith("https://globalvoices.org/feed/")
        .contains(
            "https://globalvoices.org/-/topics/sport/feed/",
            "https://globalvoices.org/-/topics/food/feed/",
            "https://globalvoices.org/-/topics/labor/feed/");
    assertThat(globalVoicesSource.getPolicy().crawling().hosts())
        .containsExactly("globalvoices.org", "www.globalvoices.org");
    assertThat(globalVoicesSource.getPolicy().crawling().contentSelectors())
        .containsExactly(".full-article .entry", ".post .entry", ".entry-container .entry");
    assertThat(globalVoicesSource.getPolicy().crawling().thumbnailSelectors()).isEmpty();
    assertThat(globalVoicesSource.getPolicy().eligibility().articleRules()).isEmpty();
    assertThat(globalVoicesSource.getPolicy().eligibility().thumbnailPolicy())
        .isEqualTo(ThumbnailPolicy.ELIGIBLE_ONLY);
    assertThat(
            globalVoicesSource
                .getPolicy()
                .eligibility()
                .thumbnailEligibility()
                .allowedCreditFragments())
        .containsExactly("Creative Commons", "CC BY", "Global Voices", "Public Domain");
    assertThat(
            globalVoicesSource
                .getPolicy()
                .eligibility()
                .thumbnailEligibility()
                .deniedCreditFragments())
        .contains("Used with permission", "Getty", "Reuters", "Associated Press", "AP Photo");
    assertThat(globalVoicesSource.isActive()).isTrue();
  }

  @Test
  void findsSeeded360infoRssSource() {
    List<ArticleSource> activeSources = articleSourceRepository.findAllByIsActiveTrue();

    ArticleSource threeSixtyInfoSource =
        activeSources.stream()
            .filter(source -> "360info".equals(source.getName()))
            .findFirst()
            .orElseThrow();

    assertThat(threeSixtyInfoSource.getId()).isEqualTo(45660871069790212L);
    assertThat(threeSixtyInfoSource.getName()).isEqualTo("360info");
    assertThat(threeSixtyInfoSource.getLanguage()).isEqualTo("en");
    assertThat(threeSixtyInfoSource.getSourceType()).isEqualTo(SourceType.RSS);
    assertThat(threeSixtyInfoSource.getPolicy().crawling().feedUrls())
        .hasSize(10)
        .startsWith("https://360info.org/feed/")
        .contains(
            "https://360info.org/category/economy/feed/",
            "https://360info.org/category/special-report/feed/");
    assertThat(threeSixtyInfoSource.getPolicy().crawling().hosts())
        .containsExactly("360info.org", "www.360info.org");
    assertThat(threeSixtyInfoSource.getPolicy().crawling().contentSelectors())
        .containsExactly("article.article .content-wrapper", "article.article");
    assertThat(threeSixtyInfoSource.getPolicy().crawling().thumbnailSelectors())
        .containsExactly(".feat-image > img");
    assertThat(threeSixtyInfoSource.getPolicy().eligibility().articleRules()).isEmpty();
    assertThat(threeSixtyInfoSource.getPolicy().eligibility().thumbnailPolicy())
        .isEqualTo(ThumbnailPolicy.ELIGIBLE_ONLY);
    assertThat(
            threeSixtyInfoSource
                .getPolicy()
                .eligibility()
                .thumbnailEligibility()
                .allowedCreditFragments())
        .containsExactly("Creative Commons", "CC BY", "360info", "Public Domain");
    assertThat(
            threeSixtyInfoSource
                .getPolicy()
                .eligibility()
                .thumbnailEligibility()
                .deniedCreditFragments())
        .contains("Getty", "Reuters", "Associated Press", "AP Photo", "third party");
    assertThat(threeSixtyInfoSource.isActive()).isTrue();
  }

  @Test
  void findsActiveSeededVoiceOfAmericaRssSource() {
    List<ArticleSource> allSources = articleSourceRepository.findAll();

    ArticleSource voiceOfAmericaSource =
        allSources.stream()
            .filter(source -> "Voice of America".equals(source.getName()))
            .findFirst()
            .orElseThrow();

    assertThat(voiceOfAmericaSource.getId()).isEqualTo(60156385693790208L);
    assertThat(voiceOfAmericaSource.getLanguage()).isEqualTo("en");
    assertThat(voiceOfAmericaSource.getSourceType()).isEqualTo(SourceType.RSS);
    assertThat(voiceOfAmericaSource.getPolicy().crawling().feedUrls())
        .containsExactly("https://www.voanews.com/api/");
    assertThat(voiceOfAmericaSource.getPolicy().crawling().hosts())
        .containsExactly("www.voanews.com", "voanews.com");
    assertThat(voiceOfAmericaSource.getPolicy().crawling().contentSelectors())
        .containsExactly("#article-content .wsw");
    assertThat(voiceOfAmericaSource.getPolicy().crawling().thumbnailSelectors()).isEmpty();

    EligibilityPolicy eligibility = voiceOfAmericaSource.getPolicy().eligibility();
    assertThat(eligibility.articleRules())
        .extracting(ArticleEligibilityRule::type)
        .containsExactly(
            RuleType.SELECTOR_EXISTS,
            RuleType.SELECTOR_ATTRIBUTE_PREFIX_ANY,
            RuleType.SELECTOR_TEXT_NOT_EQUALS_ANY,
            RuleType.DOCUMENT_HTML_NOT_CONTAINS_ANY);
    ArticleEligibilityRule hrefRule =
        eligibility.articleRules().stream()
            .filter(rule -> rule.type() == RuleType.SELECTOR_ATTRIBUTE_PREFIX_ANY)
            .findFirst()
            .orElseThrow();
    assertThat(hrefRule.selector()).isEqualTo(".publishing-details .links__item-link");
    assertThat(hrefRule.attribute()).isEqualTo("href");
    assertThat(hrefRule.values())
        .containsExactly(
            "/author/", "https://www.voanews.com/author/", "https://voanews.com/author/");
    ArticleEligibilityRule deniedBylineRule =
        eligibility.articleRules().stream()
            .filter(rule -> rule.type() == RuleType.SELECTOR_TEXT_NOT_EQUALS_ANY)
            .findFirst()
            .orElseThrow();
    assertThat(deniedBylineRule.values()).containsExactly("VOA News");
    ArticleEligibilityRule deniedHtmlRule =
        eligibility.articleRules().stream()
            .filter(rule -> rule.type() == RuleType.DOCUMENT_HTML_NOT_CONTAINS_ANY)
            .findFirst()
            .orElseThrow();
    assertThat(deniedHtmlRule.values())
        .contains(
            "wire service reports", "Associated Press", "Reuters", "Agence France-Presse", "(AFP)");
    assertThat(eligibility.thumbnailPolicy()).isEqualTo(ThumbnailPolicy.ELIGIBLE_ONLY);
    assertThat(eligibility.thumbnailEligibility().allowedCreditFragments())
        .containsExactly("VOA", "Voice of America");
    assertThat(eligibility.thumbnailEligibility().deniedCreditFragments())
        .contains("Reuters", "Associated Press", "AFP", "Getty", "Used with permission");
    assertThat(voiceOfAmericaSource.isActive()).isTrue();
  }

  @Test
  void includesVoiceOfAmericaInActiveSources() {
    List<ArticleSource> activeSources = articleSourceRepository.findAllByIsActiveTrue();

    assertThat(activeSources).anyMatch(source -> "Voice of America".equals(source.getName()));
  }
}
