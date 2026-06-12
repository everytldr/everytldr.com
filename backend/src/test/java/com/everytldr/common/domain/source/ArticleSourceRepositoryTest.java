package com.everytldr.common.domain.source;

import static org.assertj.core.api.Assertions.assertThat;

import com.everytldr.TestcontainersConfig;
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

  private static final String GLOBAL_VOICES_RSS_URL = "https://globalvoices.org/feed/";

  @Autowired private ArticleSourceRepository articleSourceRepository;

  @Test
  void findsSeededActiveGlobalVoicesRssSource() {
    List<ArticleSource> activeSources = articleSourceRepository.findAllByIsActiveTrue();

    ArticleSource globalVoicesSource =
        activeSources.stream()
            .filter(source -> GLOBAL_VOICES_RSS_URL.equals(source.getUrl()))
            .findFirst()
            .orElseThrow();

    assertThat(globalVoicesSource.getId()).isEqualTo(45660871069790211L);
    assertThat(globalVoicesSource.getName()).isEqualTo("Global Voices");
    assertThat(globalVoicesSource.getLanguage()).isEqualTo("en");
    assertThat(globalVoicesSource.getSourceType()).isEqualTo(SourceType.RSS);
    assertThat(globalVoicesSource.getPolicy().crawling().hosts())
        .containsExactly("globalvoices.org", "www.globalvoices.org");
    assertThat(globalVoicesSource.getPolicy().crawling().selectors())
        .containsExactly(".full-article .entry", ".post .entry", ".entry-container .entry");
    assertThat(globalVoicesSource.isActive()).isTrue();
  }

  @Test
  void findsSeeded360infoRssSource() {
    List<ArticleSource> activeSources = articleSourceRepository.findAllByIsActiveTrue();

    ArticleSource threeSixtyInfoSource =
        activeSources.stream()
            .filter(source -> "https://360info.org/feed/".equals(source.getUrl()))
            .findFirst()
            .orElseThrow();

    assertThat(threeSixtyInfoSource.getId()).isEqualTo(45660871069790212L);
    assertThat(threeSixtyInfoSource.getName()).isEqualTo("360info");
    assertThat(threeSixtyInfoSource.getLanguage()).isEqualTo("en");
    assertThat(threeSixtyInfoSource.getSourceType()).isEqualTo(SourceType.RSS);
    assertThat(threeSixtyInfoSource.getPolicy().crawling().hosts())
        .containsExactly("360info.org", "www.360info.org");
    assertThat(threeSixtyInfoSource.getPolicy().crawling().selectors())
        .containsExactly("article.article .content-wrapper", "article.article");
    assertThat(threeSixtyInfoSource.isActive()).isTrue();
  }
}
