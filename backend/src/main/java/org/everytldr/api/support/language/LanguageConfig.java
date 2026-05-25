package org.everytldr.api.support.language;

import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.everytldr.common.domain.language.SupportedLanguage;

@Configuration
@Profile("api")
public class LanguageConfig implements WebMvcConfigurer {
  @Bean
  public LocaleResolver localeResolver() {
    AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
    resolver.setSupportedLocales(
        Arrays.stream(SupportedLanguage.values()).map(SupportedLanguage::toLocale).toList());
    resolver.setDefaultLocale(SupportedLanguage.DEFAULT.toLocale());
    return resolver;
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(new ResolvedLanguageArgumentResolver());
  }
}
