package org.everytldr.api.support.client;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Profile("api")
public class ClientAddressConfig implements WebMvcConfigurer {
  private final String hashSecret;

  public ClientAddressConfig(@Value("${everytldr.client-address.hash-secret}") String hashSecret) {
    this.hashSecret = hashSecret;
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(new ResolvedClientAddressArgumentResolver(hashSecret));
  }
}
