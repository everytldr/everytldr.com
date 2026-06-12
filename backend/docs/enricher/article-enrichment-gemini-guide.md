# Gemini 기사 enrichment 호출 가이드

이 문서는 MVP Enricher가 Gemini Developer API로 기사 요약과 카테고리 추천을 요청하는 방식을 설명한다.
실제 저장 흐름은 `JobProcessor`와 `CompletionService`가 담당하고,
Gemini 구현은 `EnrichmentClient`의 provider adapter 역할만 한다.

## 실행 흐름

```text
DB polling이 job claim
-> ContentResolver가 원문 HTML에서 article body 생성
-> DB category slug 목록을 EnrichmentRequest.categorySlugs로 전달
-> GeminiEnrichmentClient가 runtime system prompt 로드
-> user payload JSON과 categorySlug enum schema 생성
-> Gemini generateContent 호출
-> candidates[0].content.parts[].text JSON 파싱
-> EnrichmentResult 검증
-> completion service가 article_summary와 article_category 저장 후 job 완료
```

DB category가 유일한 카테고리 source of truth다. 프롬프트나 코드에 카테고리 목록을 하드코딩하지 않고,
호출 시점의 `EnrichmentRequest.categorySlugs`만 Gemini 요청 payload와 schema enum에 넣는다.

## 설정

Spring 설정 prefix는 `everytldr.enricher.ai.gemini`다.
`ENRICHER_AI_GEMINI_ENABLED=true`일 때만 Gemini client Bean이 등록된다. true인데 API key, model,
base URL, timeout, prompt resource가 비어 있으면 애플리케이션 시작 단계에서 실패한다.

```env
ENRICHER_AI_GEMINI_ENABLED=false
ENRICHER_AI_GEMINI_BASE_URL=https://generativelanguage.googleapis.com
GEMINI_API_KEY=
ENRICHER_AI_GEMINI_MODEL=gemini-3.1-flash-lite
ENRICHER_AI_GEMINI_TIMEOUT=30s
ENRICHER_AI_GEMINI_PROMPT_PATH=classpath:prompts/article-enrichment-system-prompt.txt
ENRICHER_CACHE_CATEGORY_SLUGS_TTL=5m
ENRICHER_CACHE_ARTICLE_SOURCES_TTL=5m
```

카테고리 목록은 `Category` DB row를 source of truth로 사용한다. Enricher는 `CategorySlugProvider`를 통해
카테고리 slug 목록을 읽고, provider-local Caffeine TTL cache에 보관한 뒤 Gemini 요청 payload와 `categorySlug.enum`
schema에 넣는다. 따라서 Redis 없이도 반복 요청마다 같은 카테고리 목록을 매번 DB에서 다시 읽지 않는다.

기본 모델은 `gemini-3.1-flash-lite`다. 무료 티어와 비용 절감을 우선한 선택이며, Gemini 2.0 Flash-Lite는
2026-06-01 종료 공지가 있으므로 기본값으로 쓰지 않는다.

## 요청 형태

Gemini REST endpoint는 모델명을 path에 포함한다.

```http
POST /v1beta/models/gemini-3.1-flash-lite:generateContent
x-goog-api-key: ${GEMINI_API_KEY}
Content-Type: application/json
Accept: application/json
```

요청 body는 다음 형태다. `contents[0].parts[0].text`는 문자열이지만, 내부 값은 compact JSON이다.

```json
{
  "systemInstruction": {
    "parts": [
      {
        "text": "runtime prompt text"
      }
    ]
  },
  "contents": [
    {
      "role": "user",
      "parts": [
        {
          "text": "{\"article\":{\"contentUrl\":\"https://globalvoices.org/example\",\"source\":\"Global Voices\",\"language\":\"en\",\"body\":\"...\"},\"allowedCategorySlugs\":[\"media\",\"rights\",\"culture\"]}"
        }
      ]
    }
  ],
  "generationConfig": {
    "responseMimeType": "application/json",
    "responseJsonSchema": {
      "type": "array",
      "items": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "language": {
            "type": "string",
            "enum": ["ko", "en"]
          },
          "title": {"type": "string"},
          "summary": {"type": "string"},
          "categorySlug": {
            "type": "string",
            "enum": ["media", "rights", "culture"]
          }
        },
        "required": ["language", "title", "summary", "categorySlug"]
      },
      "minItems": 2,
      "maxItems": 2
    }
  }
}
```

## 응답 형태

Gemini의 구조화 출력은 `candidates[0].content.parts[].text` 안에 JSON 문자열로 들어온다. MVP에서는 첫 후보만 사용한다.

```json
{
  "candidates": [
    {
      "content": {
        "parts": [
          {
            "text": "[{\"language\":\"ko\",\"title\":\"시민권 운동가 인터뷰 요약\",\"summary\":\"이 기사는 지역 시민권 운동가들의 활동과 정부 대응을 다룬다.\",\"categorySlug\":\"rights\"},{\"language\":\"en\",\"title\":\"Civil Rights Activists Interview Summary\",\"summary\":\"The article covers local civil rights activists and the government response.\",\"categorySlug\":\"rights\"}]"
          }
        ],
        "role": "model"
      },
      "finishReason": "STOP"
    }
  ],
  "usageMetadata": {
    "promptTokenCount": 4000,
    "candidatesTokenCount": 500,
    "totalTokenCount": 4500
  }
}
```

저장 직전에는 다음을 다시 검증한다.

- JSON array item에 `language`, `title`, `summary`, `categorySlug`만 있는지
- `EnrichmentResult`의 필수 문자열과 title 길이 제약을 만족하는지
- 결과가 지원 언어 전체를 한 번씩 포함하고 모든 item의 `categorySlug`가 같은지
- `categorySlug`가 요청에 포함된 DB category slug인지

## 실패 분류

다음 오류는 processor의 기존 retry path로 보낸다.

- network/timeout
- HTTP `408`, `429`, `500`, `503`, `504`

다음 오류는 재시도해도 동일하게 실패할 가능성이 높으므로 permanent failure로 처리한다.

- HTTP `400`, `401`, `403`, `404`, `413`
- prompt block
- candidate 없음
- `finishReason`이 `STOP`이 아님
- output text 없음
- invalid JSON
- schema/result mismatch
- 허용되지 않은 category slug

## 무료 티어와 비용

Gemini Developer API는 모델별로 Free tier와 Paid tier가 다르다. 공식 pricing 문서 기준으로
`gemini-3.1-flash-lite`는 free tier에서 input/output이 free of charge로 표기되어 있지만, 무료 티어는
사용 가능 지역, 모델, 계정 상태, quota에 따라 달라질 수 있다.

중요한 운영 전제:

- free tier 요청은 Google 제품 개선에 사용될 수 있다.
- paid tier는 토큰 단위 과금이며, free tier와 rate limit이 다르다.
- quota 초과는 주로 HTTP `429 RESOURCE_EXHAUSTED`로 나타나며, 이번 구현에서는 retryable로 분류한다.
- 실제 운영 비용 추적은 이번 MVP 범위가 아니며 후속 PR에서 usage metadata 저장이나 metric으로 분리한다.

Paid tier 비용 산정 공식은 다음과 같다.

```text
input_cost = input_tokens / 1_000_000 * input_price
output_cost = output_tokens / 1_000_000 * output_price
total = input_cost + output_cost
```

예를 들어 paid tier에서 입력 4,000 tokens, 출력 500 tokens를 사용한다면, 실제 비용은 선택한 모델의
1M tokens당 input/output 가격을 각각 곱해 계산한다.

공식 문서:

- [Gemini pricing](https://ai.google.dev/gemini-api/docs/pricing)
- [Gemini generateContent API](https://ai.google.dev/api/generate-content)
- [Gemini structured outputs](https://ai.google.dev/gemini-api/docs/structured-output)
- [Gemini troubleshooting](https://ai.google.dev/gemini-api/docs/troubleshooting)
