# Daily Briefings — 구현 핸드오프 문서

> 이 문서는 다른 LLM 에이전트가 작업을 이어받기 위한 것입니다. 배경지식 없는 독자 기준으로 작성했습니다.

## 0. 한 줄 요약

`feature/daily-briefings` 브랜치에 "데일리 브리핑" 기능의 **코어 구현이 완료**되어 있고 모든 검증(백엔드 테스트/spotless, 프론트 tsc/eslint/steiger, 브라우저 렌더링)을 통과한 상태다. **아직 커밋하지 않았다.** 남은 일은 (A) 커밋 6개로 나눠 올리기, (B) 후속 기능 2개(홈 히어로 카드 + 기사상세 상호링크).

---

## 1. 배경 (왜 이 작업을 하는가)

Google AdSense 심사가 **"Low value content"**로 반려됐다. 근본 원인은 everytldr이 "타사 기사 1건을 AI가 요약한 파생 콘텐츠"만으로 구성되어, 독자적 가치가 없는 사이트로 판정됐기 때문이다.

AdSense 심사는 **페이지 단위가 아니라 사이트(도메인) 단위**다. 특정 광고 슬롯이 아니라 everytldr.com 전체가 반려됐다. 따라서 대응은 "사이트의 콘텐츠 정체성"을 바꾸는 것이어야 한다.

**전략**: 매일 자동으로 그날 조회수 상위 기사들을 **하나의 서술형 브리핑 문서로 종합**해 en/ko로 생성한다. 개별 요약은 원문의 파생물이지만, 여러 기사를 종합한 브리핑은 **어디에도 원본이 없는 독자 콘텐츠**다. 이것이 매일 색인 가능한 페이지로 쌓이면 사이트 정체성이 "요약 재가공"에서 "종합 콘텐츠도 생산하는 사이트"로 바뀐다.

### 확정된 설계 결정 (사용자 승인 완료)
- **기사 선정**: 조회수 순수 상위 (그날 UTC 게시분 중)
- **문서 구성**: 주제 중심 서술 (리드 문단 + 주제 섹션 3~5개)
- **날짜 기준**: UTC
- **이번 브랜치 범위**: 코어만 (백엔드 파이프라인 + API + 아카이브/상세 페이지 + 내비 탭). 홈 히어로·기사상세 상호링크는 **후속 브랜치**로 분리.

---

## 2. 브랜치 상태

- 현재 브랜치: `feature/daily-briefings`
- 분기 지점: `origin/develop` (사용자 지시로 develop에서 재분기함)
- **주의**: `related-articles` 관련 커밋(bba0888 등)은 develop에 아직 머지 안 됨. 하지만 이 브랜치는 develop에서 분기했으므로 related-articles 코드는 **없다**. 브리핑 코어는 related-articles에 의존하지 않으므로 문제없음.
- 작업 트리에 커밋 안 된 변경만 있음 (아래 3절 파일 목록).

---

## 3. 완료된 것 (파일 단위)

### 백엔드 (Spring Boot 4, Java 25, MySQL+Flyway, 프로파일 기반)

**DB 마이그레이션**
- `backend/src/main/resources/db/migration/V28__create_briefing.sql` (신규)
  - `briefing` 테이블: `(briefing_date DATE, language, title, content)`, UNIQUE `(briefing_date, language)`
  - `briefing_article` 테이블: `(briefing_date DATE, article_id FK)`, UNIQUE `(briefing_date, article_id)` — 날짜당 선정 기사 매핑. **언어 무관**. 후속 상호링크 기능의 기반.

**도메인 (`common.domain.briefing`, 신규 패키지)**
- `Briefing.java` — `BaseEntity` 상속, `rewrite()` 업서트 메서드 (ArticleSummary 패턴 미러)
- `BriefingArticle.java` — `Article`에 `@ManyToOne`
- `BriefingRepository.java` — `existsByBriefingDate`, `findByBriefingDateAndLanguage`, `findByLanguageOrderByBriefingDateDesc`, `findByLanguageAndBriefingDateLessThanOrderByBriefingDateDesc` (커서용)
- `BriefingArticleRepository.java` — `findArticleIdsByBriefingDate` (선정 순서 = id ASC 유지)

**생성 파이프라인 (`scheduler.briefing`, 신규 패키지)** — `@Profile("scheduler")`
- `BriefingSchedulingConfig.java` — `@EnableScheduling` + 전용 `ThreadPoolTaskScheduler`
- `BriefingGenerationScheduler.java` — `@Scheduled(cron="${...}", zone="UTC")`, `@ConditionalOnProperty(everytldr.briefing.generation.enabled)`
- `BriefingGenerationService.java` — **핵심 로직**: 어제(UTC) 날짜 계산 → 멱등(이미 있으면 skip) → `findMostViewedByPublishedAtBetweenAndLicenseCodeIn`로 조회수 상위 선정(en/ko 요약 모두 있는 게시가능 라이선스만) → 3건 미만이면 skip → Gemini 호출 → 저장
- `BriefingWriter.java` — `@Transactional`, 언어별 `Briefing` + `BriefingArticle` 저장
- `BriefingGenerationClient.java` (인터페이스) / `GeminiBriefingClient.java` (구현) — `GeminiEnrichmentClient` 패턴 복제. RestClient, `x-goog-api-key`, `responseJsonSchema`(언어별 배열 minItems=maxItems), 출력 검증. **enricher의 클라이언트와 의도적 중복** (enricher 무변경 원칙; common 추출 리팩토링은 범위 밖)
- `BriefingGenerationRequest/Result/Exception.java` — DTO 및 예외
- `BriefingGenerationProperties.java` / `BriefingGeminiProperties.java` — `@ConfigurationProperties`

**프롬프트**
- `backend/src/main/resources/prompts/briefing-generation-system-prompt.txt` (신규) — 주제 중심 서술 지시, 입력 사실 밖 내용 금지, cross-language consistency, 외부 서비스/모델 언급 금지

**설정** (`application-scheduler.yaml` 수정)
```yaml
everytldr:
  briefing:
    generation:
      enabled: ${BRIEFING_GENERATION_ENABLED:false}   # 기본 꺼짐
      cron: ${BRIEFING_GENERATION_CRON:0 30 * * * *}   # 매시 30분 (멱등이라 시간별 실행 = 자연 재시도, 하루 1회 성공 후 skip)
      article-count: ${BRIEFING_GENERATION_ARTICLE_COUNT:10}
    ai:
      gemini:
        enabled: ${BRIEFING_AI_GEMINI_ENABLED:false}
        api-key: ${GEMINI_API_KEY:}                    # enricher와 동일 env 재사용
        model: ${BRIEFING_AI_GEMINI_MODEL:gemini-3.1-flash-lite}
        timeout: ${BRIEFING_AI_GEMINI_TIMEOUT:60s}
        prompt-path: ${...:classpath:prompts/briefing-generation-system-prompt.txt}
```

**API (`api.briefing`, 신규 패키지)** — `@Profile("api")`
- `BriefingController.java` — `GET /api/briefings` (offset 페이지네이션, 커서는 date), `GET /api/briefings/{date}` (LocalDate 바인딩, 응답에 `articles` = 선정 기사 ListItem)
- `BriefingService.java` — 목록/상세 조회, `listArticleIds`
- `BriefingExceptions.java` — `NotFound` → `ApiExceptionHandler`에 404 매핑 추가

**기존 파일 수정**
- `ArticleRepository.java` — `findMostViewedByPublishedAtBetweenAndLicenseCodeIn` 쿼리 추가 (조회수 상위 + 기간 필터)
- `ArticleController.java` — `ArticleListResponse.Item.from`을 `package-private` → `public`으로 (BriefingController가 재사용)
- `ApiExceptionHandler.java` — `BriefingExceptions.NotFound` 핸들러 추가
- `docs/openapi.json` — `./gradlew exportOpenApi`로 재생성됨 (브리핑 엔드포인트 반영)

**테스트**
- `api/briefing/BriefingControllerTest.java` — 목록/커서/상세/404/잘못된 날짜/Accept-Language
- `scheduler/briefing/BriefingGenerationServiceTest.java` — 멱등성/기사부족 skip/조회수 정렬/언어별 저장. Gemini 클라이언트는 `@MockitoBean`
- **주의**: Article 저장 시 `article_source`에 FK가 걸려 있어 두 테스트 모두 `@BeforeEach`에서 `ArticleSource` 시드가 필요함 (이미 반영됨)

### 프론트엔드 (Next.js 16 App Router + FSD, 수정된 Next이므로 node_modules/next/dist/docs 참조 필수)

**API 클라이언트 / MSW**
- orval로 재생성됨 (`src/shared/api/client.gen.ts`, `client.gen.schemas.ts` — 커밋 대상). 수기 작성 아님. `pnpm build:orval`로 생성.
- `src/shared/api/mocks/fetchers/briefing.ts` (신규) — `listBriefings`/`getBriefing` mock 리졸버. 30일치 mock 브리핑, 서술형 mock 본문.
- `src/shared/api/mocks/fetchers/article.ts` — `ALL_ARTICLES`를 `export`로 변경 (briefing mock이 재사용)
- `src/shared/api/mocks/handlers.ts` — `*/api/briefings`, `*/api/briefings/:date` 핸들러 등록

**내비 / 카테고리 그래프**
- `src/shared/config/category.ts` — home children에 `{ slug: "briefings" }` 추가 → 서브내비 "Discover · Latest · Briefings". `STATIC_CATEGORY_SLUGS`에서 briefings 제외(전용 라우트가 서빙). `findDedicatedRouteCategorySlug(pathname)` 헬퍼 추가. **주의: `DEDICATED_ROUTE_CATEGORY_SLUGS` 상수는 `STATIC_CATEGORY_SLUGS`보다 위에 선언해야 함 (TDZ 초기화 순서 이슈로 한 번 깨졌었음)**
- `src/shared/config/index.ts` — `findDedicatedRouteCategorySlug` 배럴 export
- `src/widgets/category-nav/ui/category-nav.tsx`, `src/widgets/floating-sub-nav/ui/floating-sub-nav.tsx` — `/briefings` 라우트엔 `params.slug`가 없어 Discover가 active로 보이는 문제를, `usePathname()` + `findDedicatedRouteCategorySlug`로 보정

**페이지 슬라이스 (FSD)**
- `src/pages/briefings-archive/` — `api/fetch-briefings.ts` (`"use cache"`+`cacheLife("hours")`+`cacheTag(briefings:${locale})`+Accept-Language), `ui/briefings-archive-page.tsx` (최근 30건 단순 목록, 무한스크롤 없음, empty state), `ui/briefing-row.tsx`, `index.ts`
- `src/pages/briefing-detail/` — `api/fetch-briefing.ts` (404→notFound, `cacheTag(briefing:${locale}:${date})`), `ui/briefing-detail-page.tsx` (날짜+제목 헤더 → `MarkdownContent` 본문 → "다룬 기사" 섹션에 `ArticleList` 재사용 → `buildBriefingJsonLd`), `index.ts`

**라우트**
- `app/[locale]/(browse)/briefings/page.tsx` — 아카이브. `generateStaticParams`(locale), `buildPageMetadata({path:"/briefings"})` + `metadata.briefings`
- `app/[locale]/(browse)/briefings/[date]/page.tsx` — 상세. `__placeholder__` generateStaticParams 패턴(articles/[id] 미러), `YYYY-MM-DD` 형식 검증 후 불일치 시 `notFound()`
- `app/[locale]/(browse)/briefings/[date]/loading.tsx` — 스켈레톤
- **라우팅 확인 완료**: 정적 `briefings` 세그먼트가 `[slug]`/`[slug]/[subSlug]`보다 우선 매칭됨 (Next 문서로 검증). `/briefings`는 `briefings/page.tsx`가, `/briefings/2026-07-22`는 `briefings/[date]/page.tsx`가 처리.

**공용 lib**
- `src/shared/lib/url.ts` — `buildBriefingDetailUrl(date)` 추가
- `src/shared/lib/structured-data.ts` — `buildBriefingJsonLd` 추가 (`@type: Article`, author=Organization — 자체 콘텐츠라 정당)
- `src/shared/lib/index.ts` — 배럴 export

**sitemap / i18n**
- `app/sitemap.ts` — 최근 브리핑 50건 엔트리 추가 (priority 0.7, `fetchRecentBriefings` `"use cache"` 헬퍼)
- **i18n은 YAML 소스에서 빌드됨**: `messages/src/*.yml`을 수정하고 `pnpm build:messages`로 `messages/{en,ko}.json` 생성. **`.json`을 직접 수정하지 말 것** (덮어써짐). 추가한 키:
  - `messages/src/header.yml`: `subcategory.briefings` (Briefings/브리핑)
  - `messages/src/metadata.yml`: `briefings.{title,description}`
  - `messages/src/briefings.yml` (신규): `title`, `empty-state`, `sources-heading`

---

## 4. 검증 방법 (이미 전부 통과함, 재확인용)

### 백엔드
```bash
cd backend
./gradlew test --tests '*Briefing*'   # 브리핑만
./gradlew test                        # 전체 (통과 확인됨)
./gradlew spotlessCheck               # 포맷 (통과). 실패 시 spotlessApply 후 재확인
./gradlew exportOpenApi               # openapi.json 재생성 (API 변경 시 필수)
```
- 테스트는 Testcontainers(MySQL/Redis) 사용. Docker 필요.

### 프론트엔드
```bash
cd frontend
pnpm build:orval      # openapi.json → 클라이언트 재생성 (백엔드 API 변경 후 필수)
pnpm build:messages   # YAML → messages json
npx tsc --noEmit      # 타입체크
pnpm lint             # eslint + steiger (FSD 경계)
NEXT_PUBLIC_API_MOCKING=true pnpm dev:app   # MSW mock으로 로컬 구동
```
- 로컬 확인 URL: `/briefings`, `/briefings/<YYYY-MM-DD>` (mock은 오늘 기준 최근 30일), `/ko/briefings`
- mock 날짜는 실행일 기준이라 어제 날짜로 접근해야 200. 존재하지 않는 날짜는 정상적으로 404(Page not found).

---

## 5. 남은 작업

### A. 커밋 (아직 안 함 — 자율 커밋 금지, 각 커밋 전 사용자 승인 필요)

기능 단위 6개로 분리 제안 (커밋 메시지에 **Co-Authored-By 트레일러 넣지 말 것** — 사용자 방침):

1. **V28 + 도메인 엔티티**: `V28__create_briefing.sql`, `common/domain/briefing/*`
2. **생성 파이프라인**: `scheduler/briefing/*`, `briefing-generation-system-prompt.txt`, `application-scheduler.yaml`, `ArticleRepository`의 새 쿼리, `BriefingGenerationServiceTest`
3. **API + openapi**: `api/briefing/*`, `ApiExceptionHandler`, `ArticleController`(public 변경), `docs/openapi.json`, `BriefingControllerTest`
4. **프론트 클라이언트 + MSW**: `client.gen.*`, `mocks/fetchers/briefing.ts`, `mocks/fetchers/article.ts`, `handlers.ts`
5. **내비 + 페이지 + 라우트**: `config/category.ts`, `config/index.ts`, `widgets/*-nav`, `pages/briefings-archive`, `pages/briefing-detail`, `app/[locale]/(browse)/briefings/*`, `lib/url.ts`, `lib/structured-data.ts`, `lib/index.ts`
6. **sitemap + i18n**: `app/sitemap.ts`, `messages/src/*`, `messages/{en,ko}.json`

> 참고: 커밋 순서상 프론트(4·5)가 백엔드 API(3) 없이 빌드되려면 `client.gen.*`이 커밋에 포함돼 있어야 함(orval 산출물은 커밋 대상). 이미 생성돼 있음.

### B. 후속 기능 (이번 브랜치 범위 밖 — 별도 브랜치 권장)

심사 통과 효과를 높이려면 **심사자 동선의 모든 지점에서 브리핑이 보여야** 한다. 브리핑 전용 페이지만으로는 약하다.

**B-1. 홈 히어로 카드** (가장 중요)
- 위치: `src/pages/discover/ui/discover-page.tsx`. 카테고리 섹션들 **위에 전체 폭**으로 "오늘의 브리핑" 카드.
- 내용: 최신 브리핑의 제목 + 리드 문단 발췌 + "전체 읽기" 링크(`buildBriefingDetailUrl`).
- 데이터: `fetchBriefings(locale, 1)`로 최신 1건, 또는 상세까지 필요하면 `fetchBriefing`. 이미 `@/pages/briefings-archive`에서 `fetchBriefings` export됨.
- 카드 스타일 토큰: `rounded-md border border-hairline bg-canvas p-lg dark:bg-surface-soft` (DESIGN.md 준수, named spacing 토큰 사용).
- 근거: AdSense 심사자는 홈에서 시작. 첫 화면에 자체 종합 콘텐츠가 보이는 게 핵심.

**B-2. 기사상세 → 브리핑 상호링크**
- 위치: `src/pages/article-detail/ui/article-detail-page.tsx` (related-articles 섹션 근처). **단, 이 파일은 현재 브랜치에 없음** (related-articles가 develop에 없어서). 이 후속 작업은 related-articles가 머지된 이후에 진행하거나, related 브랜치 위에서 진행해야 함.
- 내용: "이 기사를 다룬 브리핑: N월 N일자" 한 줄 링크.
- 백엔드 지원 필요: 특정 기사가 어느 브리핑에 포함됐는지 조회. `briefing_article` 테이블에 `article_id` 인덱스가 이미 있으니 `SELECT briefing_date FROM briefing_article WHERE article_id = ?` 형태의 쿼리/엔드포인트 추가. (현재 없음 — 신규 작업)
- 효과: 심사자가 기사상세를 봐도 브리핑 존재가 보이고, 내부 링크 구조가 색인에 도움.

**B-3. (선택) 브리핑 전용 RSS**
- `app/[locale]/(browse)/briefings/feed.xml/route.ts`. 기존 `feed.xml` 라우트 패턴(`buildRssFeed`/`buildArticleFeedItem`) 재사용. 단 브리핑은 기사와 스키마가 달라 `buildBriefingFeedItem` 유사 헬퍼가 필요할 수 있음.

### C. 배포 전 운영 체크리스트 (심사 재신청 관련)
- `BRIEFING_GENERATION_ENABLED=true`, `BRIEFING_AI_GEMINI_ENABLED=true`, `GEMINI_API_KEY` 세팅해야 실제 생성됨 (기본은 전부 꺼짐).
- **브리핑이 2~3주치 쌓이고 색인된 후 재신청**할 것. 2~3건만 있는 상태로 재신청하면 효과 약함.
- 생성된 브리핑 본문이 "링크 목록"이 아니라 "서술형 종합"인지 실제 출력으로 확인 (프롬프트가 서술을 지시하지만 모델 출력 검증 필요).

---

## 6. 구현 중 발견한 함정 (재작업 방지)

1. **i18n은 YAML 소스가 진실**: `messages/{en,ko}.json`은 `pnpm build:messages` 산출물. 직접 수정 금지. `messages/src/*.yml` 수정.
2. **TDZ 초기화 순서**: `category.ts`에서 모듈 최상위 `const`가 다른 최상위 `const`의 초기화에 쓰이면 선언 순서 중요. `DEDICATED_ROUTE_CATEGORY_SLUGS`를 `STATIC_CATEGORY_SLUGS` 계산보다 위에 둬야 함.
3. **Article 저장엔 ArticleSource FK 필요**: 백엔드 테스트에서 Article 저장 전 `ArticleSource` 시드 필수 (`fk_article_source_name`).
4. **ArticleController.Item.from 가시성**: BriefingController가 재사용하므로 `public`이어야 함.
5. **Gemini 클라이언트 중복은 의도적**: enricher와 scheduler 각각 별도 클라이언트. enricher 프로파일 코드를 건드리지 않기 위함. common 추출은 하지 말 것(범위 밖, 사용자 확인 필요).
6. **수정된 Next.js**: `frontend/CLAUDE.md`가 "이건 네가 아는 Next가 아니다"라고 명시. 라우팅/캐싱은 `node_modules/next/dist/docs/` 확인 후 작업.
7. **프로젝트 규약 (CLAUDE.md)**: 코드 주석 임의 추가 금지, `render*` 프리픽스 / `onChange` 네이밍, Tailwind 클래스 inline 유지, named size 토큰, JSX 2분기 삼항, handle* 핸들러는 return 아래, 커밋에 Co-Authored-By 금지. 백엔드는 verb-first 메서드명, `assert*` 가드 등 `backend/CLAUDE.md` 참조.

---

## 7. 참고 파일

- 원래 계획서: (플랜 모드 산출물, 승인됨) — 이 문서가 그 상위 집합.
- 백엔드 패턴 원본: `scheduler/article/view/ArticleViewFlushScheduler` (cron 스케줄러 패턴), `enricher/enrichment/gemini/GeminiEnrichmentClient` (Gemini 클라이언트 패턴).
- 프론트 패턴 원본: `pages/category` (리스트 페이지), `pages/article-detail` (상세 페이지 + JSON-LD).
