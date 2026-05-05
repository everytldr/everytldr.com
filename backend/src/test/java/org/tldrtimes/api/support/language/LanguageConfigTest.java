package org.tldrtimes.api.support.language;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

class LanguageConfigTest {
  private final AcceptHeaderLocaleResolver resolver =
      (AcceptHeaderLocaleResolver) new LanguageConfig().localeResolver();

  @Test
  void resolvesSupportedAcceptLanguage() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Accept-Language", "ko-KR,en;q=0.9");
    assertThat(resolver.resolveLocale(request).toLanguageTag()).isEqualTo("ko");
  }

  @Test
  void fallsBackToEnglishWhenNoneSupported() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Accept-Language", "ja,fr;q=0.9");
    assertThat(resolver.resolveLocale(request).toLanguageTag()).isEqualTo("en");
  }
}
