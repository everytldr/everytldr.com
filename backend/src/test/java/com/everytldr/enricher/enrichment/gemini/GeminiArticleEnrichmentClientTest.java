package com.everytldr.enricher.enrichment.gemini;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.everytldr.enricher.enrichment.ArticleContent;
import com.everytldr.enricher.enrichment.ArticleEnrichmentCategoryOption;
import com.everytldr.enricher.enrichment.ArticleEnrichmentException;
import com.everytldr.enricher.enrichment.ArticleEnrichmentRequest;
import com.everytldr.enricher.enrichment.ArticleEnrichmentResult;
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

class GeminiArticleEnrichmentClientTest {
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
  void sendsGeminiRequestWithApiKeyPromptPayloadAndDynamicCategorySchema() throws Exception {
    route(200, geminiResponse(successfulOutput()));
    GeminiArticleEnrichmentClient client = newClient(serverUrl());

    ArticleEnrichmentResult result = client.enrich(request());

    assertThat(result.categorySlug()).isEqualTo("rights");
    CapturedRequest request = capturedRequest.get();
    assertThat(request.path()).isEqualTo("/v1beta/models/gemini-3.1-flash-lite:generateContent");
    assertThat(request.apiKey()).isEqualTo("test-key");

    JsonNode body = objectMapper.readTree(request.body());
    assertThat(body.at("/systemInstruction/parts/0/text").asString())
        .contains(
            "article enrichment engine",
            "article.sourceUrl, article.source, article.language, article.body",
            "source-agnostic topics",
            "Do not invent or rewrite category slugs",
            "do not over-specialize from a passing mention",
            "named-entity-specific slugs");

    JsonNode userPayload = objectMapper.readTree(body.at("/contents/0/parts/0/text").asString());
    assertThat(userPayload.at("/article/sourceUrl").asString())
        .isEqualTo("https://globalvoices.org/example");
    assertThat(userPayload.at("/allowedCategories/0/slug").asString()).isEqualTo("media");
    assertThat(userPayload.at("/allowedCategories/1/slug").asString()).isEqualTo("rights");

    JsonNode generationConfig = body.path("generationConfig");
    assertThat(generationConfig.path("responseMimeType").asString()).isEqualTo("application/json");
    JsonNode responseJsonSchema = generationConfig.path("responseJsonSchema");
    assertThat(strings(responseJsonSchema.at("/properties/categorySlug/enum")))
        .containsExactly("media", "rights");
    assertThat(strings(responseJsonSchema.at("/required")))
        .containsExactly("koTitle", "koSummary", "enTitle", "enSummary", "categorySlug");
    assertThat(responseJsonSchema.at("/additionalProperties").asBoolean()).isFalse();
  }

  @Test
  void mapsGeminiResponseToArticleEnrichmentResult() {
    route(200, geminiResponse(successfulOutput()));
    GeminiArticleEnrichmentClient client = newClient(serverUrl());

    ArticleEnrichmentResult result = client.enrich(request());

    assertThat(result)
        .isEqualTo(
            new ArticleEnrichmentResult(
                "Korean civic rights summary",
                "Korean summary describing civic rights advocacy and government response.",
                "Civic Rights Summary",
                "The article describes civic rights advocacy and the government response.",
                "rights"));
  }

  @Test
  void treatsRetryableHttpStatusesAsRetryableFailure() {
    route(429, "{}");
    GeminiArticleEnrichmentClient client = newClient(serverUrl());

    ArticleEnrichmentException exception = catchEnrichmentException(client);

    assertThat(exception).hasMessageContaining("retryable Gemini response status: 429");
    assertThat(exception.isRetryable()).isTrue();
  }

  @Test
  void treatsServerFailureAsRetryableFailure() {
    route(503, "{}");
    GeminiArticleEnrichmentClient client = newClient(serverUrl());

    ArticleEnrichmentException exception = catchEnrichmentException(client);

    assertThat(exception).hasMessageContaining("retryable Gemini response status: 503");
    assertThat(exception.isRetryable()).isTrue();
  }

  @Test
  void treatsNetworkFailureAsRetryableFailure() {
    String baseUrl = serverUrl();
    server.stop(0);
    GeminiArticleEnrichmentClient client = newClient(baseUrl);

    ArticleEnrichmentException exception = catchEnrichmentException(client);

    assertThat(exception).hasMessageContaining("failed to call Gemini API");
    assertThat(exception.isRetryable()).isTrue();
  }

  @Test
  void treatsClientFailureAsPermanentFailure() {
    route(401, "{}");
    GeminiArticleEnrichmentClient client = newClient(serverUrl());

    ArticleEnrichmentException exception = catchEnrichmentException(client);

    assertThat(exception).hasMessageContaining("non-success Gemini response status: 401");
    assertThat(exception.isRetryable()).isFalse();
  }

  @Test
  void treatsInvalidResponseJsonAsPermanentFailure() {
    route(200, "not json");
    GeminiArticleEnrichmentClient client = newClient(serverUrl());

    ArticleEnrichmentException exception = catchEnrichmentException(client);

    assertThat(exception).hasMessageContaining("Gemini response is invalid JSON");
    assertThat(exception.isRetryable()).isFalse();
  }

  @Test
  void treatsPromptBlockAsPermanentFailure() {
    route(200, "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}");
    GeminiArticleEnrichmentClient client = newClient(serverUrl());

    ArticleEnrichmentException exception = catchEnrichmentException(client);

    assertThat(exception).hasMessageContaining("Gemini prompt was blocked: SAFETY");
    assertThat(exception.isRetryable()).isFalse();
  }

  @Test
  void treatsNoOutputAsPermanentFailure() {
    route(200, "{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":[]}}]}");
    GeminiArticleEnrichmentClient client = newClient(serverUrl());

    ArticleEnrichmentException exception = catchEnrichmentException(client);

    assertThat(exception).hasMessageContaining("Gemini response has no output text");
    assertThat(exception.isRetryable()).isFalse();
  }

  @Test
  void treatsNonStopFinishReasonAsPermanentFailure() {
    route(
        200,
        """
        {"candidates":[{"finishReason":"MAX_TOKENS","content":{"parts":[{"text":"{}"}]}}]}
        """);
    GeminiArticleEnrichmentClient client = newClient(serverUrl());

    ArticleEnrichmentException exception = catchEnrichmentException(client);

    assertThat(exception).hasMessageContaining("Gemini finishReason is not STOP: MAX_TOKENS");
    assertThat(exception.isRetryable()).isFalse();
  }

  @Test
  void treatsUnexpectedOutputFieldAsPermanentFailure() {
    route(
        200,
        geminiResponse(
            """
            {
              "koTitle": "Korean civic rights summary",
              "koSummary": "Korean summary describing civic rights advocacy.",
              "enTitle": "Civic Rights Summary",
              "enSummary": "The article describes civic rights advocacy.",
              "categorySlug": "rights",
              "extra": "unexpected"
            }
            """));
    GeminiArticleEnrichmentClient client = newClient(serverUrl());

    ArticleEnrichmentException exception = catchEnrichmentException(client);

    assertThat(exception).hasMessageContaining("Gemini output fields do not match expected schema");
    assertThat(exception.isRetryable()).isFalse();
  }

  @Test
  void treatsUnknownCategoryAsPermanentFailure() {
    route(
        200,
        geminiResponse(
            """
            {
              "koTitle": "Korean civic rights summary",
              "koSummary": "Korean summary describing civic rights advocacy.",
              "enTitle": "Civic Rights Summary",
              "enSummary": "The article describes civic rights advocacy.",
              "categorySlug": "unknown-category"
            }
            """));
    GeminiArticleEnrichmentClient client = newClient(serverUrl());

    ArticleEnrichmentException exception = catchEnrichmentException(client);

    assertThat(exception).hasMessageContaining("Gemini categorySlug is not allowed");
    assertThat(exception.isRetryable()).isFalse();
  }

  private GeminiArticleEnrichmentClient newClient(String baseUrl) {
    return new GeminiArticleEnrichmentClient(
        RestClient.builder(),
        objectMapper,
        new EnricherGeminiProperties(
            true,
            baseUrl,
            "test-key",
            "gemini-3.1-flash-lite",
            Duration.ofSeconds(2),
            "classpath:prompts/article-enrichment-system-prompt.txt"),
        new DefaultResourceLoader());
  }

  private ArticleEnrichmentException catchEnrichmentException(
      GeminiArticleEnrichmentClient client) {
    Throwable thrown = catchThrowable(() -> client.enrich(request()));
    assertThat(thrown).isInstanceOf(ArticleEnrichmentException.class);
    return (ArticleEnrichmentException) thrown;
  }

  private ArticleEnrichmentRequest request() {
    return new ArticleEnrichmentRequest(
        new ArticleContent(
            "https://globalvoices.org/example",
            "Global Voices",
            "en",
            "Local civic rights advocates described new community organizing efforts. ".repeat(20)),
        List.of(
            new ArticleEnrichmentCategoryOption("media"),
            new ArticleEnrichmentCategoryOption("rights")));
  }

  private String successfulOutput() {
    return """
    {
      "koTitle": "Korean civic rights summary",
      "koSummary": "Korean summary describing civic rights advocacy and government response.",
      "enTitle": "Civic Rights Summary",
      "enSummary": "The article describes civic rights advocacy and the government response.",
      "categorySlug": "rights"
    }
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
        ],
        "usageMetadata": {
          "promptTokenCount": 100,
          "candidatesTokenCount": 50,
          "totalTokenCount": 150
        }
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
