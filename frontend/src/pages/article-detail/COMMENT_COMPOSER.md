# 댓글 작성(Composer) 기능 — 구현 요구사항 핸드오프

> **상태: 구현 완료 (2026-06-01).** 본 명세에 따라 전 항목 구현됨. tsc / ESLint / Steiger(FSD) 모두 통과. 아래 §4 작업 내역은 실제 구현된 파일과 일치하며, 향후 변경 시 참고용으로 유지.

> 다른 LLM 에이전트가 이어서 구현하기 위한 명세서. 모든 의사결정은 사용자가 확정했으며, 임의로 변경하지 말 것. 새로운 설계 분기가 생기면 코드 작성 전에 사용자에게 먼저 질문할 것.

## 구현 결과 (FSD 배치)

전 기능을 `pages/article-detail/` 슬라이스에 배치(단일 페이지 전용 → 추출 불필요, FSD Golden Rule). 인프라성만 `shared/`:

- `shared/ui/textarea.tsx` — Textarea 프리미티브 (신규)
- `shared/api/mocks/fetchers/article.ts` + `handlers.ts` — `createArticleComment` POST 핸들러 (인메모리)
- `pages/article-detail/ui/comment-composer.tsx` — 작성 폼 (client, 신규)
- `pages/article-detail/ui/comment-list.tsx` — 인터랙티브 댓글 영역(상단 composer + 트리 + 인라인 답글), `CommentNode` 소유 (client, 신규)
- `pages/article-detail/ui/article-comments.tsx` — 서버 컴포넌트: fetch + 트리 빌드 + `CommentList` 렌더

> 경계 메모: 서버(`ArticleComments`)는 `CommentList`(client)만 import. 함수 prop을 가진 `CommentComposer`는 `CommentList` 안에서만 사용 → Next 71007(serializable props) 경고 회피.

## 0. 작업 전 필독

- 루트 `CLAUDE.md`(frontend) 규칙을 **반드시** 준수: UI 컴포넌트 Props 규약(§1), shadcn 우선(§2), `Nullable/Optional/Maybe`(§3), `Translation` 우선(§4), duration 상수(§5).
- 이 프로젝트의 Next.js는 변형 버전. 코드 작성 전 `node_modules/next/dist/docs/` 관련 가이드 확인.
- **주석 금지**: `//`, `/* */` 를 사용자 승인 없이 추가하지 말 것.
- Tailwind 클래스는 인라인으로만(반복 체인을 상수로 추출 금지).
- 핸들러 함수(`handleX`)는 JSX `return` **아래**에 함수 선언으로 작성(호이스팅 의존). 기존 `article-like-button.tsx` 참고.
- 값 변경 핸들러 이름은 `onChange`(`onValueChange` 아님), render-prop은 `render` 접두사.
- 작업 완료 후 `pnpm lint` 통과 및 포맷 정리 필수.

## 1. 기능 개요

기사 상세 페이지의 댓글 영역에 **익명 댓글 작성 폼(composer)** 과 **인라인 답글 기능**을 추가한다. everytldr는 유동닉 방식(작성 시마다 닉네임 + 비밀번호 입력, `maskedIp` 노출)이다.

대상 슬라이스: `src/pages/article-detail/`
현재 댓글 목록은 `src/pages/article-detail/ui/article-comments.tsx` (서버 컴포넌트, 읽기 전용)에 구현되어 있음.

## 2. 확정된 의사결정 (변경 금지)

| 항목 | 결정 |
| --- | --- |
| 폼 위치 | 댓글 목록 **위(상단)** — 제목/카운트 바로 아래, 목록 위 |
| 필드 배치 | **내용 textarea(주인공)를 위에**, 그 아래 한 줄에 **닉네임 · 비밀번호 · 등록 버튼** |
| 답글 | **포함** — 인라인 답글 폼 |
| 답글 깊이 | **1단계만** — 최상위(root) 댓글에만 답글 버튼. 답글의 답글 불가 |
| 답글 폼 동시 열림 | **한 번에 하나만** (다른 답글 폼 열면 기존 폼 닫힘) — `activeReplyId` state |
| 필수 입력 | **닉네임 · 비밀번호 · 내용 3개 모두 필수** |
| content 검증 | 공백 제외 **최소 1자**, 최대 5000자 |
| password 검증 | **최소 4자**, 최대 100자 (모델 제약) |
| nickname 검증 | 최대 50자 |
| 글자 수 카운터 | content에 `0/5000` 형태로 노출 |
| 비밀번호 표시 토글 | **제외**(아래 §7 참고) — 일반 `type="password"` Input 사용 |
| 제출 후 갱신 | **클라이언트 mutation(`useCreateArticleComment`) + `router.refresh()`** |
| Mock | **인메모리 저장** (작성 → 조회 시 반영, 서버 재시작 전까지 유지) |

## 3. 현재까지 완료된 작업

- [x] **Textarea 프리미티브 추가**: `src/shared/ui/textarea.tsx` 생성 + `src/shared/ui/index.ts`에 export 완료.
  - DESIGN.md §3.4(textarea = `rounded-md`), Input 패턴(`ring-1 ring-inset ring-hairline-strong`, `hover:ring-ink`, `focus-visible:ring-2 ring-primary`, `disabled`, `aria-invalid:ring-semantic-error`)을 미러링함.
  - `TextareaProps = ComponentProps<"textarea">` (className 포함, §1.1 충족).

남은 작업: §4 ~ §6.

## 4. 남은 구현 작업

### 4.1. Mock POST 핸들러 (인메모리 저장)

**파일**: `src/shared/api/mocks/fetchers/article.ts`

- `createArticleComment` async 핸들러 추가. msw 시그니처: `async ({ request, params: { articleId } }: { request: Request; params: { articleId: string } })`.
- 동작:
  1. `findArticle(articleId)` 없으면 `new HttpResponse(null, { status: 404 })`.
  2. `const body = (await request.json()) as ArticleCommentCreateRequest`.
  3. 기존 `COMMENTS_BY_ARTICLE_ID`(현재 파일 32번째 줄, `Map<string, ArticleCommentListItem[]>`)에서 해당 기사 배열을 가져와 새 댓글 push.
  4. 새 댓글 `ArticleCommentListItem` 생성:
     - `id`: 기존 id 충돌 방지. 예: `(BigInt(articleId) + BigInt(1000 + comments.length)).toString()` (기존 mock은 `+1`, `+2` 사용 중).
     - `content`: `body.content`
     - `createdAt`: `new Date().toISOString()`
     - `maskedIp`: 임의값 (예: `"192.0.2.*"`)
     - `nickname`: `body.nickname`
     - `parentId`: `body.parentId`
  5. `HttpResponse.json(newComment, { status: 201 })` 반환 (201 응답 data는 `ArticleCommentListItem` — `client.gen.ts:659`).
- `ArticleCommentCreateRequest` 타입을 파일 상단 `import type` 블록(현재 1~8줄)에 추가.

**파일**: `src/shared/api/mocks/handlers.ts`

- import에 `createArticleComment` 추가.
- 핸들러 배열에 `http.post("*/api/articles/:articleId/comments", createArticleComment)` 추가.

> 참고: 기존 `likeArticle`/`unlikeArticle`이 모듈 스코프 `Set`/`Map` 변경으로 인메모리 상태를 유지하는 패턴과 동일하게 구현.

### 4.2. i18n 키 추가

**파일**: `messages/src/article-detail.yml` (YAML, `{ en, ko }` leaf 구조)

추가할 키 (값은 자연스러운 한국어/영어로):
- `comment-placeholder` (내용 textarea placeholder)
- `comment-nickname` (닉네임 input placeholder/label)
- `comment-password` (비밀번호 input placeholder/label)
- `comment-submit` (등록 버튼)
- `comment-reply` (답글 버튼)
- `comment-reply-cancel` (답글 취소 버튼)
- `comment-char-count` — 예: `"{count}/5000"`
- `comment-error-required` (필수 입력 누락)
- `comment-error-password-min` (비밀번호 최소 4자)
- `comment-submit-error` (작성 실패)

작성 후 **반드시** `pnpm build:messages` 실행 (yml → `messages/{en,ko}.json` 컴파일). dev 모드면 `dev:messages` watcher가 자동 처리.

> placeholder는 native 속성이라 plain string 필요 → 클라이언트 컴포넌트에서 `const t = useTranslations("article-detail")` 후 `t("comment-placeholder")` 사용. JSX 텍스트는 `Translation` 우선(CLAUDE §4.1).

### 4.3. CommentComposer 클라이언트 컴포넌트 (신규)

**파일**: `src/pages/article-detail/ui/comment-composer.tsx` (`"use client"`)

최상위 댓글과 답글 양쪽에서 재사용하는 공용 폼.

**Props** (`CommentComposerProps`, §1.1 준수, `className?: string` 포함):
- `className?: string`
- `articleId: string`
- `parentId?: string` — 답글일 때 전달
- `onSuccess?: () => void` — 작성 성공 후 호출(답글 폼 닫기용)
- `onCancel?: () => void` — 존재하면 "취소" 버튼 노출(답글 모드)
- `autoFocus?: boolean` — 답글 폼 열릴 때 textarea 포커스

**레이아웃** (확정된 배치):
```
┌─────────────────────────────────────┐
│ <Textarea> 내용 입력 (주인공, 상단)    │
│                          [n/5000]    │  ← char counter (우하단 정렬)
├─────────────────────────────────────┤
│ [닉네임 Input] [비밀번호 Input] [등록] │  ← 한 줄, 등록은 우측
│                         (답글이면 [취소])│
└─────────────────────────────────────┘
```
- 내용: `Textarea` (`@/shared/ui`).
- 닉네임: `Input` (`@/shared/ui`), `maxLength={50}`.
- 비밀번호: `Input` `type="password"`, `maxLength={100}`.
- 등록 버튼: `Button variant="primary"` (`@/shared/ui`).
- 취소 버튼(답글 모드): `Button variant="ghost"` 또는 `secondary`.
- 글자 수 카운터: `text-caption-mono text-meta` (DESIGN.md §3.2.2, 숫자=mono).

**상태/검증**:
- `content`, `nickname`, `password` 로컬 state.
- 검증: 3필드 모두 필수, `content.trim().length >= 1`, `password.length >= 4`.
- 에러는 `aria-invalid="true"`로 트리거(DESIGN.md §5.2.2, class-only 금지). 헬퍼 텍스트는 `text-caption text-semantic-error`.
- 제출 중 버튼 `disabled`(`mutation.isPending`).

**제출 로직**:
- `useCreateArticleComment()` (`@/shared/api`) 사용. mutation 변수 형태: `{ articleId, data: ArticleCommentCreateRequest }` (`client.gen.ts:713` 참고).
- 성공(201) 시: 폼 초기화 → `router.refresh()` → `onSuccess?.()`.
- `useRouter`는 **`@/shared/i18n`** 에서 import (locale-aware, `epl-team-filter.tsx` 참고). `next/navigation` 아님.
- `router.refresh()`로 서버 컴포넌트(`ArticleComments`) 재검증되어 새 댓글이 목록에 반영됨.
- 핸들러는 `handleSubmit` 등으로 JSX return 아래 함수 선언.

> 참고 구현 패턴: `src/pages/article-detail/ui/article-like-button.tsx` (mutation + 클라이언트 컴포넌트 구조).

### 4.4. CommentList 클라이언트 컴포넌트 (신규)

**파일**: `src/pages/article-detail/ui/comment-list.tsx` (`"use client"`)

현재 `article-comments.tsx`의 `CommentItem` 렌더링(서버)을 클라이언트로 이전. 이유: 답글 폼 토글("한 번에 하나만") 상태 공유 필요.

**Props** (`CommentListProps`):
- `className?: string`
- `articleId: string`
- `nodes: CommentNode[]` — 서버에서 `buildCommentTree`로 만든 트리(직렬화 가능한 plain object)
- `locale: string` — `formatDate(comment.createdAt, locale)`용

**동작**:
- `const [activeReplyId, setActiveReplyId] = useState<Maybe<string>>(null)` (CLAUDE §3.1).
- 최상위(root) 댓글에만 "답글" 버튼(`Button variant="link"` 또는 ghost) 렌더.
- 답글 버튼 클릭 → `setOpenReplyId(prev => prev === id ? null : id)` (토글, 하나만 열림).
- 열린 댓글 아래 `CommentComposer`(`parentId={comment.id}`, `onSuccess`/`onCancel`로 `setOpenReplyId(null)`, `autoFocus`) 인라인 렌더.
- 자식(답글) 노드는 기존처럼 들여쓰기 렌더하되 **답글 버튼 없음**(1단계 제한).
- `CommentNode` 타입과 `formatDate` 표시 로직은 기존 `article-comments.tsx`에서 그대로 이전.

> 대안: `CommentItem`을 별도 파일로 더 쪼개도 됨. 단 컴포넌트별 `{Component}Props` 규약 유지.

### 4.5. ArticleComments 통합

**파일**: `src/pages/article-detail/ui/article-comments.tsx` (서버 컴포넌트 유지)

- 기존 fetch(`listArticleComments`) + `buildCommentTree` + locale 취득 로직 유지.
- 제목/카운트 영역 **아래, 목록 위**에 `<CommentComposer articleId={articleId} />` (최상위 폼) 배치.
- 기존 `<ol>` 목록 렌더를 `<CommentList articleId nodes={nodes} locale={locale} />` 로 교체.
- `buildCommentTree`/`CommentNode`/`findParent`는 `CommentList`로 옮길지(클라) 서버에 남길지 선택:
  - 권장: **서버에서 트리 빌드** 후 `nodes`만 클라로 전달(클라 번들 최소화). `CommentNode` 타입은 공유 위치(예: `comment-list.tsx`에서 export)로.
- `ArticleCommentsSkeleton`은 그대로 유지. composer 자리에 스켈레톤 추가할지는 선택(필수 아님).
- `index.ts` public API에 필요한 export 확인.

## 5. 데이터 모델 / API 레퍼런스

- 생성 API (orval 생성, **이미 존재**): `POST /api/articles/{articleId}/comments`
  - fetch: `createArticleComment(articleId, body)` — `client.gen.ts:696`
  - mutation 훅: `useCreateArticleComment()` — `client.gen.ts:743`
  - 변수: `{ articleId: string; data: ArticleCommentCreateRequest }`
- `ArticleCommentCreateRequest` (`client.gen.schemas.ts:8`):
  ```ts
  { content?: string;   // 0~5000
    nickname?: string;  // 0~50
    parentId?: string;
    password?: string } // 4~100
  ```
- `ArticleCommentListItem` (`client.gen.schemas.ts:27`):
  ```ts
  { content?: string; createdAt?: string; id?: string;
    maskedIp?: string; nickname?: string; parentId?: string }
  ```
- 조회 API: `listArticleComments(articleId)` (`@/shared/api`), 200 시 `data.items`.

## 6. 수용 기준 (Acceptance)

1. 댓글 영역 상단에 작성 폼(내용 위, 닉/비번/등록 아래)이 보인다.
2. 3필드 중 하나라도 비거나 content 공백/ password 4자 미만이면 제출 불가 + `aria-invalid` 에러 표시.
3. 정상 제출 시 폼이 비워지고 목록 최하단(또는 트리 적절 위치)에 새 댓글이 즉시 반영된다(`router.refresh`).
4. 최상위 댓글에만 "답글" 버튼이 있고, 클릭 시 인라인 답글 폼이 열린다. 다른 답글 버튼을 누르면 기존 폼은 닫힌다.
5. 답글 작성 시 해당 댓글의 자식으로 1단계 들여쓰기 렌더된다. 답글에는 답글 버튼이 없다.
6. 새로고침해도 작성한 댓글이 유지된다(인메모리 mock).
7. 라이트/다크 양쪽에서 DESIGN.md 토큰만 사용(하드코딩 hex/px 없음).
8. `pnpm lint` 통과.

## 7. 미해결/확인 필요 사항 (구현자가 사용자에게 물을 것)

- **비밀번호 표시/숨김 토글 제외 사유**: 공유 `Input` 컴포넌트(`input.tsx`)는 내용이 있을 때 우측에 기본 clear(X) 버튼을 렌더한다. 여기에 eye 토글을 더하면 우측 영역이 충돌하므로 기본값 토글을 제외했다. 토글이 꼭 필요하면 `Input` 확장 또는 전용 필드가 필요 → 사용자 확인 후 진행.
- 새 댓글의 목록 내 정렬/위치(시간 오름/내림차순) 명시 안 됨 — 기존 mock 순서(append) 따름. 변경 원하면 확인.
