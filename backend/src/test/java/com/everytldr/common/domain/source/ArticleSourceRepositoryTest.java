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
    assertThat(globalVoicesSource.getLicenseInfo().getLicenseCode()).isEqualTo(LicenseCode.CC_BY);
    assertThat(globalVoicesSource.getLicenseInfo().getLicenseVersion()).isEqualTo("3.0");
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
    assertThat(threeSixtyInfoSource.getLicenseInfo().getLicenseCode()).isEqualTo(LicenseCode.CC_BY);
    assertThat(threeSixtyInfoSource.getLicenseInfo().getLicenseVersion()).isEqualTo("4.0");
    assertThat(threeSixtyInfoSource.isActive()).isTrue();
  }
}
