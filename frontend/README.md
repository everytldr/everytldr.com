# everytldr Frontend

Next.js 앱입니다. 코드는 Feature-Sliced Design 레이어(`app` / `pages` / `widgets` / `entities` / `shared`)로 구성되어 있습니다.

## 실행

pnpm이 필요합니다. 버전은 `package.json`의 `packageManager`를 따릅니다.

```bash
pnpm install
cp .env.example .env
pnpm dev
```

http://localhost:3000 에서 열립니다. `.env` 기본값은 API 목킹(MSW)이 켜져 있어서 백엔드 없이도 화면을 볼 수 있습니다. 실제 백엔드에 붙이려면 `NEXT_PUBLIC_API_MOCKING=false`로 바꾸고 `BACKEND_URL`에 백엔드 주소를 넣어주세요.

`pnpm dev`는 next dev와 함께 메시지 번들 워처, steiger(FSD 경계 검사), orval(API 타입 생성) 워처를 같이 띄웁니다.

## 기술 스택

- Next.js 16 (App Router), React 19, TypeScript
- Tailwind CSS 4
- TanStack Query + orval — `docs/openapi.json`에서 API 타입·훅 자동 생성
- next-intl — 한국어/영어 로컬라이즈드 URL
- MSW — API 목킹
- steiger — FSD 임포트 경계 린트
