package com.everytldr.scheduler.briefing;

import com.everytldr.common.domain.language.SupportedLanguage;
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
@Profile("scheduler")
@ConditionalOnProperty(name = "everytldr.briefing.ai.gemini.enabled", havingValue = "true")
public class GeminiBriefingClient implements BriefingGenerationClient {
  private static final List<String> RESULT_FIELDS = List.of("language", "title", "content");
  private static final List<String> SUPPORTED_LANGUAGE_CODES =
      Arrays.stream(SupportedLanguage.values()).map(SupportedLanguage::code).toList();

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final BriefingGeminiProperties properties;
  private final String systemPrompt;

  public GeminiBriefingClient(
      RestClient.Builder restClientBuilder,
      ObjectMapper objectMapper,
      BriefingGeminiProperties properties,
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
  public List<Result> generate(Request request) {
    String payload = serializePayload(request);
    GeminiHttpResponse response = callGemini(payload);
    return parseResult(response);
  }

  private GeminiHttpResponse callGemini(String userPayload) {
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
                buildResponseSchema()));

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
      throw new BriefingGenerationException("failed to call Gemini API", e);
    }
  }

  private List<Result> parseResult(GeminiHttpResponse response) {
    int statusCode = response.statusCode();
    boolean isSuccess = statusCode >= 200 && statusCode < 300;
    if (!isSuccess) {
      throw new BriefingGenerationException("non-success Gemini response status: " + statusCode);
    }

    JsonNode body = parseJson(response.body());
    JsonNode blockReason = body.at("/promptFeedback/blockReason");

    boolean isPromptBlocked = blockReason.isString() && !blockReason.asString().isBlank();
    if (isPromptBlocked) {
      throw new BriefingGenerationException("Gemini prompt was blocked: " + blockReason.asString());
    }

    JsonNode candidates = body.path("candidates");
    if (!candidates.isArray() || candidates.isEmpty()) {
      throw new BriefingGenerationException("Gemini response has no candidates");
    }

    JsonNode firstCandidate = candidates.get(0);
    assertFinishReasonIsStop(firstCandidate.path("finishReason"));

    JsonNode output = parseJson(extractOutput(firstCandidate));
    assertExpectedOutputShape(output);

    List<Result> results = toResults(output);
    results.forEach(Result::assertValid);
    assertCoversSupportedLanguages(results);
    return results;
  }

  private String extractOutput(JsonNode candidate) {
    JsonNode parts = candidate.at("/content/parts");
    if (!parts.isArray()) {
      throw new BriefingGenerationException("Gemini response has no output parts");
    }

    String outputText =
        StreamSupport.stream(parts.spliterator(), false)
            .map(part -> part.path("text"))
            .filter(JsonNode::isString)
            .map(JsonNode::asString)
            .filter(text -> !text.isBlank())
            .collect(Collectors.joining());
    if (outputText.isBlank()) {
      throw new BriefingGenerationException("Gemini response has no output text");
    }
    return outputText;
  }

  private JsonNode parseJson(String json) {
    try {
      return objectMapper.readTree(json);
    } catch (JacksonException e) {
      throw new BriefingGenerationException("Gemini returned invalid JSON", e);
    }
  }

  private List<Result> toResults(JsonNode output) {
    return StreamSupport.stream(output.spliterator(), false)
        .map(
            item -> {
              try {
                return objectMapper.treeToValue(item, Result.class);
              } catch (JacksonException e) {
                throw new BriefingGenerationException("Gemini output schema mismatch", e);
              }
            })
        .toList();
  }

  private String serializePayload(Request request) {
    try {
      return objectMapper.writeValueAsString(GeminiUserPayload.from(request));
    } catch (JacksonException e) {
      throw new BriefingGenerationException("failed to serialize Gemini user payload", e);
    }
  }

  private Map<String, Object> buildResponseSchema() {
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
                Map.of(
                    "type",
                    "string",
                    "description",
                    "Thematic briefing headline in the specified language."),
                "content",
                Map.of(
                    "type",
                    "string",
                    "description",
                    "Narrative Markdown briefing with themed sections, written in the specified language.")),
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

  private SimpleClientHttpRequestFactory createRequestFactory(BriefingGeminiProperties properties) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(properties.timeout());
    requestFactory.setReadTimeout(properties.timeout());
    return requestFactory;
  }

  private void assertFinishReasonIsStop(JsonNode finishReasonNode) {
    if (!finishReasonNode.isString()) {
      throw new BriefingGenerationException("Gemini response is missing finishReason");
    }

    String finishReason = finishReasonNode.asString();
    if ("STOP".equals(finishReason)) {
      return;
    }
    throw new BriefingGenerationException("Gemini finishReason is not STOP: " + finishReason);
  }

  private void assertExpectedOutputShape(JsonNode output) {
    if (!output.isArray() || output.isEmpty()) {
      throw new BriefingGenerationException("Gemini output is not a non-empty JSON array");
    }

    Set<String> expectedFields = Set.copyOf(RESULT_FIELDS);
    for (JsonNode item : output) {
      Set<String> fieldNames = new HashSet<>(item.propertyNames());
      if (!fieldNames.equals(expectedFields)) {
        throw new BriefingGenerationException(
            "Gemini output item fields do not match expected schema");
      }
    }
  }

  private void assertCoversSupportedLanguages(List<Result> results) {
    Set<String> returnedLanguages =
        results.stream().map(Result::language).collect(Collectors.toSet());
    Set<String> supportedLanguages = new HashSet<>(SUPPORTED_LANGUAGE_CODES);

    if (!returnedLanguages.equals(supportedLanguages)) {
      throw new BriefingGenerationException(
          "Gemini languages do not match supported set: " + returnedLanguages);
    }
  }

  private record GeminiHttpResponse(int statusCode, String body) {}

  private record GeminiUserPayload(List<GeminiArticlePayload> articles) {
    static GeminiUserPayload from(Request request) {
      Objects.requireNonNull(request, "request must not be null");
      List<GeminiArticlePayload> articles =
          request.articles().stream()
              .map(article -> new GeminiArticlePayload(article.title(), article.summary()))
              .toList();
      return new GeminiUserPayload(articles);
    }

    private record GeminiArticlePayload(String title, String summary) {}
  }
}
