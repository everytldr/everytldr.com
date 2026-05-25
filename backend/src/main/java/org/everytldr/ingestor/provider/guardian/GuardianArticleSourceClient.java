package org.everytldr.ingestor.provider.guardian;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.everytldr.common.domain.source.ArticleSource;
import org.everytldr.common.domain.source.SourceType;
import org.everytldr.ingestor.provider.ArticleSourceClient;
import org.everytldr.ingestor.provider.CollectedArticle;

@Component
@Profile("ingestor")
public class GuardianArticleSourceClient implements ArticleSourceClient {
  private static final String SEARCH_PATH = "/search";

  private final RestClient restClient;
  private final ClientProperties properties;
  private final GuardianArticleMapper guardianArticleMapper;

  public GuardianArticleSourceClient(
      RestClient.Builder restClientBuilder,
      ClientProperties properties,
      GuardianArticleMapper guardianArticleMapper) {
    this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
    this.properties = properties;
    this.guardianArticleMapper = guardianArticleMapper;
  }

  @Override
  public boolean supports(SourceType sourceType) {
    return sourceType == SourceType.GUARDIAN_API;
  }

  @Override
  public List<CollectedArticle> collect(ArticleSource source) {
    if (!StringUtils.hasText(properties.apiKey())) {
      throw new IllegalStateException("Guardian API key is required");
    }

    String section = extractSection(source);
    GuardianSearchResponse response =
        restClient
            .get()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path(SEARCH_PATH)
                        .queryParam("section", section)
                        .queryParam("order-by", "newest")
                        .queryParam("show-fields", "thumbnail")
                        .queryParam("page-size", properties.pageSize())
                        .queryParam("api-key", properties.apiKey())
                        .build())
            .retrieve()
            .body(GuardianSearchResponse.class);

    return guardianArticleMapper.map(response, source);
  }

  private String extractSection(ArticleSource source) {
    URI locator = URI.create(source.getUrl());
    String query = locator.getRawQuery();
    if (!StringUtils.hasText(query)) {
      throw new IllegalArgumentException(
          "Guardian article source url must include section query parameter");
    }

    for (String parameter : query.split("&")) {
      String[] pair = parameter.split("=", 2);
      if (pair.length == 2 && "section".equals(pair[0])) {
        String section = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
        if (StringUtils.hasText(section)) {
          return section;
        }
      }
    }

    throw new IllegalArgumentException(
        "Guardian article source url must include section query parameter");
  }

  @ConfigurationProperties(prefix = "everytldr.ingestor.guardian")
  public record ClientProperties(String baseUrl, String apiKey, int pageSize) {}
}
