package com.everytldr.enricher.enrichment.gemini;

import com.everytldr.enricher.enrichment.ArticleContent;
import com.everytldr.enricher.enrichment.ArticleEnrichmentCategoryOption;
import com.everytldr.enricher.enrichment.ArticleEnrichmentClient;
import com.everytldr.enricher.enrichment.ArticleEnrichmentException;
import com.everytldr.enricher.enrichment.ArticleEnrichmentRequest;
import com.everytldr.enricher.enrichment.ArticleEnrichmentResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
public class GeminiArticleEnrichmentClient implements ArticleEnrichmentClient {
  private static final Set<String> RESULT_FIELDS =
      Set.of("koTitle", "koSummary", "enTitle", "enSummary", "categorySlug");

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final EnricherGeminiProperties properties;
  private final String systemPrompt;

  public GeminiArticleEnrichmentClient(
      RestClient.Builder restClientBuilder,
      ObjectMapper objectMapper,
      EnricherGeminiProperties properties,
      ResourceLoader resourceLoader) {
    this.objectMapper = objectMapper;
    this.properties = properties;
    this.systemPrompt = loadPrompt(resourceLoader, properties.promptResource());
    this.restClient =
        restClientBuilder
            .baseUrl(properties.baseUrl())
            .requestFactory(requestFactory(properties))
            .build();
  }

  @Override
  public ArticleEnrichmentResult enrich(ArticleEnrichmentRequest request) {
    List<String> allowedCategorySlugs =
        request.categories().stream().map(ArticleEnrichmentCategoryOption::slug).toList();
    String userPayload = userPayload(request);
    GeminiHttpResponse response = callGemini(request, userPayload);
    return parseResult(response, allowedCategorySlugs);
  }

  private GeminiHttpResponse callGemini(ArticleEnrichmentRequest request, String userPayload) {
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
                responseSchema(request.categories())));

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
      throw ArticleEnrichmentException.retryable("failed to call Gemini API", e);
    }
  }

  private ArticleEnrichmentResult parseResult(
      GeminiHttpResponse response, List<String> allowedCategorySlugs) {
    if (isRetryableStatus(response.statusCode())) {
      throw ArticleEnrichmentException.retryable(
          "retryable Gemini response status: " + response.statusCode());
    }
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw ArticleEnrichmentException.permanent(
          "non-success Gemini response status: " + response.statusCode());
    }

    JsonNode root = parseJson(response.body(), "Gemini response is invalid JSON");
    JsonNode promptBlockReason = root.at("/promptFeedback/blockReason");
    if (promptBlockReason.isString() && !promptBlockReason.asString().isBlank()) {
      throw ArticleEnrichmentException.permanent(
          "Gemini prompt was blocked: " + promptBlockReason.asString());
    }

    JsonNode candidates = root.path("candidates");
    if (!candidates.isArray() || candidates.isEmpty()) {
      throw ArticleEnrichmentException.permanent("Gemini response has no candidates");
    }

    JsonNode firstCandidate = candidates.get(0);
    validateFinishReason(firstCandidate.path("finishReason"));
    String outputText = outputText(firstCandidate);
    JsonNode output = parseJson(outputText, "Gemini output text is invalid JSON");
    validateOutputShape(output);

    ArticleEnrichmentResult result = toResult(output);
    result
        .validationErrorMessage()
        .ifPresent(
            message -> {
              throw ArticleEnrichmentException.permanent("Gemini output is invalid: " + message);
            });
    if (!allowedCategorySlugs.contains(result.categorySlug())) {
      throw ArticleEnrichmentException.permanent(
          "Gemini categorySlug is not allowed: " + result.categorySlug());
    }
    return result;
  }

  private void validateFinishReason(JsonNode finishReasonNode) {
    if (!finishReasonNode.isString()) {
      throw ArticleEnrichmentException.permanent("Gemini response is missing finishReason");
    }

    String finishReason = finishReasonNode.asString();
    if ("STOP".equals(finishReason)) {
      return;
    }
    throw ArticleEnrichmentException.permanent("Gemini finishReason is not STOP: " + finishReason);
  }

  private String outputText(JsonNode candidate) {
    JsonNode parts = candidate.at("/content/parts");
    if (!parts.isArray()) {
      throw ArticleEnrichmentException.permanent("Gemini response has no output parts");
    }

    String outputText =
        StreamSupport.stream(parts.spliterator(), false)
            .map(part -> part.path("text"))
            .filter(JsonNode::isString)
            .map(JsonNode::asString)
            .filter(text -> !text.isBlank())
            .reduce("", String::concat);
    if (outputText.isBlank()) {
      throw ArticleEnrichmentException.permanent("Gemini response has no output text");
    }
    return outputText;
  }

  private JsonNode parseJson(String json, String message) {
    try {
      return objectMapper.readTree(json);
    } catch (JacksonException e) {
      throw ArticleEnrichmentException.permanent(message, e);
    }
  }

  private void validateOutputShape(JsonNode output) {
    if (!output.isObject()) {
      throw ArticleEnrichmentException.permanent("Gemini output is not a JSON object");
    }

    Set<String> fieldNames = new HashSet<>(output.propertyNames());
    if (!fieldNames.equals(RESULT_FIELDS)) {
      throw ArticleEnrichmentException.permanent(
          "Gemini output fields do not match expected schema");
    }
  }

  private ArticleEnrichmentResult toResult(JsonNode output) {
    try {
      return objectMapper.treeToValue(output, ArticleEnrichmentResult.class);
    } catch (JacksonException e) {
      throw ArticleEnrichmentException.permanent("Gemini output schema mismatch", e);
    }
  }

  private String userPayload(ArticleEnrichmentRequest request) {
    try {
      return objectMapper.writeValueAsString(GeminiArticleEnrichmentUserPayload.from(request));
    } catch (JacksonException e) {
      throw ArticleEnrichmentException.permanent("failed to serialize Gemini user payload", e);
    }
  }

  private Map<String, Object> responseSchema(List<ArticleEnrichmentCategoryOption> categories) {
    return Map.of(
        "type",
        "object",
        "additionalProperties",
        false,
        "properties",
        Map.of(
            "koTitle",
            Map.of("type", "string", "description", "Korean article title."),
            "koSummary",
            Map.of("type", "string", "description", "Korean article summary."),
            "enTitle",
            Map.of("type", "string", "description", "English article title."),
            "enSummary",
            Map.of("type", "string", "description", "English article summary."),
            "categorySlug",
            Map.of(
                "type",
                "string",
                "description",
                "One allowed category slug.",
                "enum",
                categories.stream().map(ArticleEnrichmentCategoryOption::slug).toList())),
        "required",
        List.of("koTitle", "koSummary", "enTitle", "enSummary", "categorySlug"));
  }

  private boolean isRetryableStatus(int statusCode) {
    return statusCode == 408
        || statusCode == 429
        || statusCode == 500
        || statusCode == 503
        || statusCode == 504;
  }

  private String loadPrompt(ResourceLoader resourceLoader, String promptResource) {
    Resource resource = resourceLoader.getResource(promptResource);
    try {
      return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException(
          "failed to load Gemini prompt resource: " + promptResource, e);
    }
  }

  private SimpleClientHttpRequestFactory requestFactory(EnricherGeminiProperties properties) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(properties.requestTimeout());
    requestFactory.setReadTimeout(properties.requestTimeout());
    return requestFactory;
  }

  private record GeminiHttpResponse(int statusCode, String body) {}

  private record GeminiArticleEnrichmentUserPayload(
      ArticleContent article, List<ArticleEnrichmentCategoryOption> allowedCategories) {
    static GeminiArticleEnrichmentUserPayload from(ArticleEnrichmentRequest request) {
      Objects.requireNonNull(request, "request must not be null");
      return new GeminiArticleEnrichmentUserPayload(request.content(), request.categories());
    }
  }
}
