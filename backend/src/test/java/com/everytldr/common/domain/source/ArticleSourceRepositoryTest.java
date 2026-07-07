package com.everytldr.common.domain.source;

import static org.assertj.core.api.Assertions.assertThat;

import com.everytldr.TestcontainersConfig;
import com.everytldr.common.domain.license.LicenseCode;
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
    List<ArticleSource> activeSources = articleSourceRepository.findAllByIsActiveTrueOrderByIdAsc();

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
    assertThat(globalVoicesSource.getLicenseInfo().getLicenseCode()).isEqualTo(LicenseCode.CC_BY);
    assertThat(globalVoicesSource.getLicenseInfo().getLicenseVersion()).isEqualTo("3.0");
    assertThat(globalVoicesSource.isActive()).isTrue();
  }

  @Test
  void findsSeeded360infoRssSource() {
    List<ArticleSource> activeSources = articleSourceRepository.findAllByIsActiveTrueOrderByIdAsc();

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
    assertThat(threeSixtyInfoSource.getLicenseInfo().getLicenseCode()).isEqualTo(LicenseCode.CC_BY);
    assertThat(threeSixtyInfoSource.getLicenseInfo().getLicenseVersion()).isEqualTo("4.0");
    assertThat(threeSixtyInfoSource.isActive()).isTrue();
  }

  @Test
  void findsSeededCcByRssSources() {
    List<ArticleSource> activeSources = articleSourceRepository.findAllByIsActiveTrueOrderByIdAsc();

    ArticleSource universeToday = findSource(activeSources, "Universe Today");
    assertThat(universeToday.getId()).isEqualTo(62693100752990208L);
    assertThat(universeToday.getPolicy().crawling().feedUrls())
        .containsExactly("https://www.universetoday.com/rss.xml");
    assertThat(universeToday.getPolicy().crawling().hosts())
        .containsExactly("universetoday.com", "www.universetoday.com");
    assertThat(universeToday.getPolicy().crawling().contentSelectors())
        .containsExactly(".article-content");
    assertCcBy4(universeToday);

    ArticleSource effDeeplinks = findSource(activeSources, "EFF Deeplinks");
    assertThat(effDeeplinks.getId()).isEqualTo(62693100752990209L);
    assertThat(effDeeplinks.getPolicy().crawling().feedUrls())
        .containsExactly("https://www.eff.org/rss/updates.xml");
    assertThat(effDeeplinks.getPolicy().crawling().hosts()).containsExactly("www.eff.org");
    assertThat(effDeeplinks.getPolicy().crawling().contentSelectors())
        .containsExactly(".node--full .field--name-body");
    assertCcBy4(effDeeplinks);

    ArticleSource apc = findSource(activeSources, "APC");
    assertThat(apc.getId()).isEqualTo(62693100752990210L);
    assertThat(apc.getPolicy().crawling().feedUrls())
        .containsExactly("https://www.apc.org/en/rss.xml");
    assertThat(apc.getPolicy().crawling().hosts()).containsExactly("www.apc.org");
    assertThat(apc.getPolicy().crawling().contentSelectors()).containsExactly(".field--name-body");
    assertCcBy4(apc);

    ArticleSource horizonMagazine = findSource(activeSources, "Horizon Magazine");
    assertThat(horizonMagazine.getId()).isEqualTo(62693100752990211L);
    assertThat(horizonMagazine.getPolicy().crawling().feedUrls())
        .containsExactly(
            "https://projects.research-and-innovation.ec.europa.eu/horizon-magazine/articles.xml");
    assertThat(horizonMagazine.getPolicy().crawling().hosts())
        .containsExactly("projects.research-and-innovation.ec.europa.eu");
    assertThat(horizonMagazine.getPolicy().crawling().contentSelectors())
        .containsExactly(".article--body");
    assertThat(horizonMagazine.getPolicy().crawling().allowedPathPrefixes())
        .containsExactly("/en/horizon-magazine/");
    assertCcBy4(horizonMagazine);

    ArticleSource sciDevNet = findSource(activeSources, "SciDev.Net");
    assertThat(sciDevNet.getId()).isEqualTo(62693100752990212L);
    assertThat(sciDevNet.getPolicy().crawling().feedUrls())
        .containsExactly("https://www.scidev.net/global/rss.xml/?type=header");
    assertThat(sciDevNet.getPolicy().crawling().hosts()).containsExactly("www.scidev.net");
    assertThat(sciDevNet.getPolicy().crawling().contentSelectors())
        .containsExactly(".fl-module-fl-post-content .fl-module-content");
    assertThat(sciDevNet.getPolicy().crawling().allowedPathPrefixes())
        .containsExactly(
            "/global/news/",
            "/global/features/",
            "/global/opinions/",
            "/global/scidev-net-investigates/");
    assertThat(sciDevNet.getLicenseInfo().getLicenseCode()).isEqualTo(LicenseCode.CC_BY);
    assertThat(sciDevNet.getLicenseInfo().getLicenseVersion()).isEqualTo("2.0");
    assertThat(sciDevNet.isActive()).isTrue();
  }

  private ArticleSource findSource(List<ArticleSource> sources, String name) {
    return sources.stream()
        .filter(source -> name.equals(source.getName()))
        .findFirst()
        .orElseThrow();
  }

  private void assertCcBy4(ArticleSource source) {
    assertThat(source.getLicenseInfo().getLicenseCode()).isEqualTo(LicenseCode.CC_BY);
    assertThat(source.getLicenseInfo().getLicenseVersion()).isEqualTo("4.0");
    assertThat(source.isActive()).isTrue();
  }
}
