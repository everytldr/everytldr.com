package org.everytldr.api.support.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("api")
public class OpenApiConfig {
  @Bean
  public OpenAPI everytldrOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("everytldr API")
                .description("Public API for the everytldr reader experience.")
                .version("v0"));
  }
}
