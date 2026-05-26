package org.everytldr.common.domain.language;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SupportedLanguageTest {

  @Test
  void fromCodeAcceptsSupportedTagsIncludingRegionAndCase() {
    assertThat(SupportedLanguage.fromCode("ko")).contains(SupportedLanguage.KOREAN);
    assertThat(SupportedLanguage.fromCode("EN")).contains(SupportedLanguage.ENGLISH);
    assertThat(SupportedLanguage.fromCode("ko-KR")).contains(SupportedLanguage.KOREAN);
    assertThat(SupportedLanguage.fromCode("en_US")).contains(SupportedLanguage.ENGLISH);
  }

  @Test
  void fromCodeReturnsEmptyForUnknownOrBlankInput() {
    assertThat(SupportedLanguage.fromCode("ja")).isEmpty();
    assertThat(SupportedLanguage.fromCode("")).isEmpty();
    assertThat(SupportedLanguage.fromCode(null)).isEmpty();
  }
}
