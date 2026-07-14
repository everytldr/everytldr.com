package com.everytldr.api.support.visitor;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Profile("api")
@RequiredArgsConstructor
public class AnonymousVisitorConfig implements WebMvcConfigurer {
  private final AnonymousVisitorProperties properties;

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(new ResolvedAnonymousVisitorArgumentResolver(properties));
  }
}
