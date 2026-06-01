package com.everytldr.api.support.language;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Locale;
import com.everytldr.common.domain.language.SupportedLanguage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.MethodParameter;

class ResolvedLanguageArgumentResolverTest {
  private final ResolvedLanguageArgumentResolver resolver = new ResolvedLanguageArgumentResolver();

  @AfterEach
  void resetLocale() {
    LocaleContextHolder.resetLocaleContext();
  }

  @Test
  void supportsRequiresAnnotationAndSupportedLanguageType() throws NoSuchMethodException {
    assertThat(resolver.supportsParameter(parameterOf("annotated", SupportedLanguage.class)))
        .isTrue();
    assertThat(resolver.supportsParameter(parameterOf("unannotated", SupportedLanguage.class)))
        .isFalse();
    assertThat(resolver.supportsParameter(parameterOf("wrongType", String.class))).isFalse();
  }

  @Test
  void resolvesCurrentLocaleWithEnglishFallback() throws Exception {
    LocaleContextHolder.setLocale(Locale.KOREAN);
    assertThat(resolve()).isEqualTo(SupportedLanguage.KOREAN);

    LocaleContextHolder.setLocale(Locale.JAPANESE);
    assertThat(resolve()).isEqualTo(SupportedLanguage.ENGLISH);
  }

  private Object resolve() throws Exception {
    return resolver.resolveArgument(
        parameterOf("annotated", SupportedLanguage.class), null, null, null);
  }

  private static MethodParameter parameterOf(String methodName, Class<?> paramType)
      throws NoSuchMethodException {
    Method method = Fixtures.class.getDeclaredMethod(methodName, paramType);
    return new MethodParameter(method, 0);
  }

  @SuppressWarnings("unused")
  private static class Fixtures {
    void annotated(@ResolvedLanguage SupportedLanguage language) {}

    void unannotated(SupportedLanguage language) {}

    void wrongType(@ResolvedLanguage String language) {}
  }
}
