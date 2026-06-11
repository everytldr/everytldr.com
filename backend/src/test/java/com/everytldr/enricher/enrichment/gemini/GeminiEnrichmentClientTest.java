package com.everytldr.enricher.enrichment.gemini;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.everytldr.enricher.enrichment.EnrichmentException;
import com.everytldr.enricher.enrichment.EnrichmentRequest;
import com.everytldr.enricher.enrichment.EnrichmentResult;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class GeminiEnrichmentClientTest {
  private final ObjectMapper objectMapper = new ObjectMapper();

  private HttpServer server;
  private AtomicReference<CapturedRequest> capturedRequest;

  @BeforeEach
  void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    capturedRequest = new AtomicReference<>();
    server.start();
  }

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void sendsGeminiRequestAndMapsValidResponse() throws Exception {
    route(200, geminiResponse(successfulOutput()));

    List<EnrichmentResult> results = newClient().enrich(request());

    assertThat(results)
        .containsExactly(
            new EnrichmentResult(
                "ko",
                "Korean civic rights summary",
                "Korean summary describing civic rights advocacy.",
                "global-voices-rights"),
            new EnrichmentResult(
                "en",
                "Civic Rights Summary",
                "The article describes civic rights advocacy.",
                "global-voices-rights"));

    CapturedRequest request = capturedRequest.get();
    assertThat(request.path()).isEqualTo("/v1beta/models/gemini-3.1-flash-lite:generateContent");
    assertThat(request.apiKey()).isEqualTo("test-key");

    JsonNode body = objectMapper.readTree(request.body());
    assertThat(body.at("/systemInstruction/parts/0/text").asString())
        .contains("article enrichment engine");

    JsonNode userPayload = objectMapper.readTree(body.at("/contents/0/parts/0/text").asString());
    assertThat(userPayload.at("/article/contentUrl").asString())
        .isEqualTo("https://globalvoices.org/example");
    assertThat(strings(userPayload.path("allowedCategorySlugs")))
        .containsExactly("global-voices", "global-voices-rights");

    JsonNode categoryEnum =
        body.at("/generationConfig/responseJsonSchema/items/properties/categorySlug/enum");
    assertThat(strings(categoryEnum)).containsExactly("global-voices", "global-voices-rights");
  }

  @Test
  void retryableHttpStatusThrowsRetryableException() {
    route(429, "{}");

    EnrichmentException exception = catchEnrichmentException();

    assertThat(exception).hasMessageContaining("retryable Gemini response status: 429");
    assertThat(exception.isRetryable()).isTrue();
  }

  @Test
  void nonRetryableHttpStatusThrowsPermanentException() {
    route(401, "{}");

    EnrichmentException exception = catchEnrichmentException();

    assertThat(exception).hasMessageContaining("non-success Gemini response status: 401");
    assertThat(exception.isRetryable()).isFalse();
  }

  @Test
  void invalidModelOutputThrowsPermanentException() {
    route(200, geminiResponse("[{\"language\":\"ko\",\"title\":\"Title\"}]"));

    EnrichmentException exception = catchEnrichmentException();

    assertThat(exception)
        .hasMessageContaining("Gemini output item fields do not match expected schema");
    assertThat(exception.isRetryable()).isFalse();
  }

  @Test
  void disallowedCategoryThrowsPermanentException() {
    route(
        200,
        geminiResponse(
            """
            [
              {
                "language": "ko",
                "title": "Korean civic rights summary",
                "summary": "Korean summary describing civic rights advocacy.",
                "categorySlug": "unknown-category"
              },
              {
                "language": "en",
                "title": "Civic Rights Summary",
                "summary": "The article describes civic rights advocacy.",
                "categorySlug": "unknown-category"
              }
            ]
            """));

    EnrichmentException exception = catchEnrichmentException();

    assertThat(exception).hasMessageContaining("Gemini categorySlug is not allowed");
    assertThat(exception.isRetryable()).isFalse();
  }

  @Test
  void mismatchedCategoriesThrowPermanentException() {
    route(
        200,
        geminiResponse(
            """
            [
              {
                "language": "ko",
                "title": "Korean civic rights summary",
                "summary": "Korean summary describing civic rights advocacy.",
                "categorySlug": "global-voices"
              },
              {
                "language": "en",
                "title": "Civic Rights Summary",
                "summary": "The article describes civic rights advocacy.",
                "categorySlug": "global-voices-rights"
              }
            ]
            """));

    EnrichmentException exception = catchEnrichmentException();

    assertThat(exception).hasMessageContaining("Gemini categorySlug values do not match");
    assertThat(exception.isRetryable()).isFalse();
  }

  private GeminiEnrichmentClient newClient() {
    return new GeminiEnrichmentClient(
        RestClient.builder(),
        objectMapper,
        new GeminiProperties(
            true,
            serverUrl(),
            "test-key",
            "gemini-3.1-flash-lite",
            Duration.ofSeconds(2),
            "classpath:prompts/article-enrichment-system-prompt.txt"),
        new DefaultResourceLoader());
  }

  private EnrichmentException catchEnrichmentException() {
    Throwable thrown = catchThrowable(() -> newClient().enrich(request()));
    assertThat(thrown).isInstanceOf(EnrichmentException.class);
    return (EnrichmentException) thrown;
  }

  private EnrichmentRequest request() {
    return new EnrichmentRequest(
        "https://globalvoices.org/example",
        "Global Voices",
        "en",
        "Local civic rights advocates described new community organizing efforts. ".repeat(20),
        List.of("global-voices", "global-voices-rights"));
  }

  private String successfulOutput() {
    return """
        [
          {
            "language": "ko",
            "title": "Korean civic rights summary",
            "summary": "Korean summary describing civic rights advocacy.",
            "categorySlug": "global-voices-rights"
          },
          {
            "language": "en",
            "title": "Civic Rights Summary",
            "summary": "The article describes civic rights advocacy.",
            "categorySlug": "global-voices-rights"
          }
        ]
        """;
  }

  private String geminiResponse(String outputJson) {
    try {
      return """
          {
            "candidates": [
              {
                "content": {
                  "parts": [
                    {
                      "text": %s
                    }
                  ],
                  "role": "model"
                },
                "finishReason": "STOP"
              }
            ]
          }
          """
          .formatted(objectMapper.writeValueAsString(outputJson));
    } catch (JacksonException e) {
      throw new IllegalStateException(e);
    }
  }

  private List<String> strings(JsonNode node) {
    return StreamSupport.stream(node.spliterator(), false).map(JsonNode::asString).toList();
  }

  private String serverUrl() {
    return "http://localhost:%d".formatted(server.getAddress().getPort());
  }

  private void route(int status, String body) {
    server.createContext(
        "/",
        exchange -> {
          byte[] requestBody = exchange.getRequestBody().readAllBytes();
          capturedRequest.set(
              new CapturedRequest(
                  exchange.getRequestURI().getPath(),
                  exchange.getRequestHeaders().getFirst("x-goog-api-key"),
                  new String(requestBody, StandardCharsets.UTF_8)));
          byte[] responseBody = body.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(status, responseBody.length);
          try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(responseBody);
          }
        });
  }

  private record CapturedRequest(String path, String apiKey, String body) {}
}
