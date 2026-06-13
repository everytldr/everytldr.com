package com.everytldr.enricher.enrichment.gemini;

import com.everytldr.common.domain.language.SupportedLanguage;
import com.everytldr.enricher.enrichment.EnrichmentClient;
import com.everytldr.enricher.enrichment.EnrichmentException;
import com.everytldr.enricher.enrichment.EnrichmentRequest;
import com.everytldr.enricher.enrichment.EnrichmentResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@Profile("enricher")
@ConditionalOnProperty(name = "everytldr.enricher.ai.gemini.enabled", havingValue = "true")
public class GeminiEnrichmentClient implements EnrichmentClient {
  private static final List<String> RESULT_FIELDS =
      List.of("language", "title", "summary", "categorySlug");
  private static final List<String> SUPPORTED_LANGUAGE_CODES =
      Arrays.stream(SupportedLanguage.values()).map(SupportedLanguage::code).toList();

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final GeminiProperties properties;
  private final String systemPrompt;

  public GeminiEnrichmentClient(
      RestClient.Builder restClientBuilder,
      ObjectMapper objectMapper,
      GeminiProperties properties,
      ResourceLoader resourceLoader) {
    this.objectMapper = objectMapper;
    this.properties = properties;
    this.systemPrompt = loadPrompt(resourceLoader, properties.promptPath());
    this.restClient =
        restClientBuilder
            .baseUrl(properties.baseUrl())
            .requestFactory(createRequestFactory(properties))
            .build();
  }

  @Override
  public List<EnrichmentResult> enrich(EnrichmentRequest request) {
    String payload = serializePayload(request);
    GeminiHttpResponse response = callGemini(request, payload);
    return parseResult(response, request.categorySlugs());
  }

  private GeminiHttpResponse callGemini(EnrichmentRequest request, String userPayload) {
    Map<String, Object> body =
        Map.of(
            "systemInstruction",
            Map.of("parts", List.of(Map.of("text", systemPrompt))),
            "contents",
            List.of(Map.of("role", "user", "parts", List.of(Map.of("text", userPayload)))),
            "generationConfig",
            Map.of(
                "responseMimeType",
                "application/json",
                "responseJsonSchema",
                buildResponseSchema(request.categorySlugs())));

    try {
      return restClient
          .post()
          .uri("/v1beta/models/{model}:generateContent", properties.model())
          .header("x-goog-api-key", properties.apiKey())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .body(body)
          .exchange(
              (httpRequest, httpResponse) ->
                  new GeminiHttpResponse(
                      httpResponse.getStatusCode().value(),
                      StreamUtils.copyToString(httpResponse.getBody(), StandardCharsets.UTF_8)));
    } catch (RestClientException e) {
      throw EnrichmentException.retryable("failed to call Gemini API", e);
    }
  }

  private List<EnrichmentResult> parseResult(
      GeminiHttpResponse response, List<String> allowedCategorySlugs) {
    int statusCode = response.statusCode();
    boolean isRetryableStatus =
        statusCode == 408
            || statusCode == 429
            || statusCode == 500
            || statusCode == 503
            || statusCode == 504;
    if (isRetryableStatus) {
      throw EnrichmentException.retryable("retryable Gemini response status: " + statusCode);
    }

    boolean isSuccess = statusCode >= 200 && statusCode < 300;
    if (!isSuccess) {
      throw EnrichmentException.permanent("non-success Gemini response status: " + statusCode);
    }

    JsonNode body = parseJson(response.body());
    JsonNode blockReason = body.at("/promptFeedback/blockReason");

    boolean isPromptBlocked = blockReason.isString() && !blockReason.asString().isBlank();
    if (isPromptBlocked) {
      throw EnrichmentException.permanent("Gemini prompt was blocked: " + blockReason.asString());
    }

    JsonNode candidates = body.path("candidates");
    if (!candidates.isArray() || candidates.isEmpty()) {
      throw EnrichmentException.permanent("Gemini response has no candidates");
    }

    JsonNode firstCandidate = candidates.get(0);
    assertFinishReasonIsStop(firstCandidate.path("finishReason"));

    JsonNode output = parseJson(extractOutput(firstCandidate));
    assertExpectedOutputShape(output);

    List<EnrichmentResult> results = toResults(output);
    results.forEach(
        result -> {
          result.assertValid();
          assertCategoryAllowed(result, allowedCategorySlugs);
        });
    assertSingleCategory(results);
    assertCoversSupportedLanguages(results);
    return results;
  }

  private String extractOutput(JsonNode candidate) {
    JsonNode parts = candidate.at("/content/parts");
    if (!parts.isArray()) {
      throw EnrichmentException.permanent("Gemini response has no output parts");
    }

    String outputText =
        StreamSupport.stream(parts.spliterator(), false)
            .map(part -> part.path("text"))
            .filter(JsonNode::isString)
            .map(JsonNode::asString)
            .filter(text -> !text.isBlank())
            .collect(Collectors.joining());
    if (outputText.isBlank()) {
      throw EnrichmentException.permanent("Gemini response has no output text");
    }
    return outputText;
  }

  private JsonNode parseJson(String json) {
    try {
      return objectMapper.readTree(json);
    } catch (JacksonException e) {
      throw EnrichmentException.permanent("Gemini returned invalid JSON", e);
    }
  }

  private List<EnrichmentResult> toResults(JsonNode output) {
    return StreamSupport.stream(output.spliterator(), false)
        .map(
            item -> {
              try {
                return objectMapper.treeToValue(item, EnrichmentResult.class);
              } catch (JacksonException e) {
                throw EnrichmentException.permanent("Gemini output schema mismatch", e);
              }
            })
        .toList();
  }

  private String serializePayload(EnrichmentRequest request) {
    try {
      return objectMapper.writeValueAsString(GeminiUserPayload.from(request));
    } catch (JacksonException e) {
      throw EnrichmentException.permanent("failed to serialize Gemini user payload", e);
    }
  }

  private Map<String, Object> buildResponseSchema(List<String> categorySlugs) {
    Map<String, Object> itemSchema =
        Map.of(
            "type",
            "object",
            "additionalProperties",
            false,
            "properties",
            Map.of(
                "language",
                Map.of(
                    "type",
                    "string",
                    "description",
                    "Output language code.",
                    "enum",
                    SUPPORTED_LANGUAGE_CODES),
                "title",
                Map.of("type", "string", "description", "Article title in the specified language."),
                "summary",
                Map.of(
                    "type",
                    "string",
                    "description",
                    "Scannable sectioned Markdown digest with bullets, written in the specified language."),
                "categorySlug",
                Map.of(
                    "type",
                    "string",
                    "description",
                    "One allowed category slug.",
                    "enum",
                    categorySlugs)),
            "required",
            RESULT_FIELDS);
    return Map.of(
        "type",
        "array",
        "items",
        itemSchema,
        "minItems",
        SUPPORTED_LANGUAGE_CODES.size(),
        "maxItems",
        SUPPORTED_LANGUAGE_CODES.size());
  }

  private String loadPrompt(ResourceLoader resourceLoader, String promptPath) {
    Resource resource = resourceLoader.getResource(promptPath);
    try {
      return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("failed to load Gemini prompt resource: " + promptPath, e);
    }
  }

  private SimpleClientHttpRequestFactory createRequestFactory(GeminiProperties properties) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(properties.timeout());
    requestFactory.setReadTimeout(properties.timeout());
    return requestFactory;
  }

  private void assertFinishReasonIsStop(JsonNode finishReasonNode) {
    if (!finishReasonNode.isString()) {
      throw EnrichmentException.permanent("Gemini response is missing finishReason");
    }

    String finishReason = finishReasonNode.asString();
    if ("STOP".equals(finishReason)) {
      return;
    }
    throw EnrichmentException.permanent("Gemini finishReason is not STOP: " + finishReason);
  }

  private void assertExpectedOutputShape(JsonNode output) {
    if (!output.isArray() || output.isEmpty()) {
      throw EnrichmentException.permanent("Gemini output is not a non-empty JSON array");
    }

    Set<String> expectedFields = Set.copyOf(RESULT_FIELDS);
    for (JsonNode item : output) {
      Set<String> fieldNames = new HashSet<>(item.propertyNames());
      if (!fieldNames.equals(expectedFields)) {
        throw EnrichmentException.permanent(
            "Gemini output item fields do not match expected schema");
      }
    }
  }

  private void assertCategoryAllowed(EnrichmentResult result, List<String> allowedCategorySlugs) {
    if (!allowedCategorySlugs.contains(result.categorySlug())) {
      throw EnrichmentException.permanent(
          "Gemini categorySlug is not allowed: " + result.categorySlug());
    }
  }

  private void assertSingleCategory(List<EnrichmentResult> results) {
    long categoryCount = results.stream().map(EnrichmentResult::categorySlug).distinct().count();
    if (categoryCount != 1) {
      throw EnrichmentException.permanent("Gemini categorySlug values do not match");
    }
  }

  private void assertCoversSupportedLanguages(List<EnrichmentResult> results) {
    Set<String> returnedLanguages =
        results.stream().map(EnrichmentResult::language).collect(Collectors.toSet());
    Set<String> supportedLanguages = new HashSet<>(SUPPORTED_LANGUAGE_CODES);

    if (!returnedLanguages.equals(supportedLanguages)) {
      throw EnrichmentException.permanent(
          "Gemini languages do not match supported set: " + returnedLanguages);
    }
  }

  private record GeminiHttpResponse(int statusCode, String body) {}

  private record GeminiUserPayload(
      GeminiArticlePayload article, List<String> allowedCategorySlugs) {
    static GeminiUserPayload from(EnrichmentRequest request) {
      Objects.requireNonNull(request, "request must not be null");
      return new GeminiUserPayload(
          new GeminiArticlePayload(
              request.contentUrl(), request.source(), request.language(), request.content()),
          request.categorySlugs());
    }

    private record GeminiArticlePayload(
        String contentUrl, String source, String language, String body) {}
  }
}
