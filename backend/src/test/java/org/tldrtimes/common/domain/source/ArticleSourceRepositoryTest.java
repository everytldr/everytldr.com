package org.tldrtimes.common.domain.source;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.tldrtimes.TestcontainersConfig;

@SpringBootTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
@Transactional
class ArticleSourceRepositoryTest {

  private static final String GUARDIAN_FOOTBALL_URL =
      "https://content.guardianapis.com/search?section=football";

  @Autowired private ArticleSourceRepository articleSourceRepository;

  @Test
  void findsSeededActiveGuardianFootballSource() {
    List<ArticleSource> activeSources = articleSourceRepository.findAllByIsActiveTrue();

    ArticleSource guardianFootballSource =
        activeSources.stream()
            .filter(source -> GUARDIAN_FOOTBALL_URL.equals(source.getUrl()))
            .findFirst()
            .orElseThrow();

    assertThat(guardianFootballSource.getId()).isEqualTo(45660871069790209L);
    assertThat(guardianFootballSource.getName()).isEqualTo("The Guardian Football");
    assertThat(guardianFootballSource.getLanguage()).isEqualTo("en");
    assertThat(guardianFootballSource.getSourceType()).isEqualTo(SourceType.GUARDIAN_API);
    assertThat(guardianFootballSource.isActive()).isTrue();
  }
}
