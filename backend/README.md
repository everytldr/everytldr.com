# everytldr Backend

Spring Boot 서버입니다. 하나의 Gradle 프로젝트를 Spring 프로필로 역할을 나눠 실행합니다.

## 프로필

| 프로필 | 역할 |
| --- | --- |
| `api` | REST API, 익명 방문자 식별, 조회수·인기 기사 조회 |
| `scheduler` | Redis 조회수를 MySQL에 반영하고 반영 이력을 정리 |
| `ingestor` | RSS에서 새 기사 메타데이터를 수집하는 Spring Batch 작업 |
| `enricher` | 기사 본문을 가져와 AI 요약과 카테고리 분류를 처리 |
| `monolith` | 위 네 프로필을 함께 활성화하는 기본 프로필 |

수집 폴링은 `ingestor` 프로필에서 `INGESTION_ENABLED=true`일 때, 요약 폴링은 `enricher` 프로필에서 `ENRICHER_PROCESSING_ENABLED=true`일 때만 실행됩니다. `scheduler`의 조회수 반영 작업은 `ARTICLE_VIEW_FLUSH_ENABLED`로 제어합니다.

## 실행

Java 25와 Docker가 필요합니다. 로컬 Compose는 MySQL, Redis, Prometheus, Grafana를 실행합니다.

먼저 환경 파일을 만들고 `VISITOR_HASH_SECRET`에 로컬 전용 임의 문자열을 설정하세요. 이 값은 익명 방문자 식별용 HMAC 비밀값입니다.

```bash
cp .env.example .env
# .env의 VISITOR_HASH_SECRET을 설정
docker compose -f docker-compose.local.yml up -d
```

`monolith`이 기본 프로필이므로 별도 지정 없이 네 역할이 함께 실행됩니다. 기본 실행과 역할 분리 실행 예시는 다음과 같습니다.

```bash
# 기본 프로필: monolith
./gradlew bootRun

# 역할 분리: API만 실행
./gradlew bootRun --args='--spring.profiles.active=api'
```

첫 부팅 때 Flyway가 테이블을 생성합니다. 기본 DB 접속값은 `docker-compose.local.yml`의 계정과 일치합니다.

수집부터 AI 요약까지 체험하려면 `.env`에서 다음 설정을 추가로 켜고 Gemini API 키를 입력하세요.

```properties
INGESTION_ENABLED=true
ENRICHER_PROCESSING_ENABLED=true
ENRICHER_AI_GEMINI_ENABLED=true
GEMINI_API_KEY=...
```

## 로컬 주소와 API 문서

- API: <http://localhost:8080>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Prometheus metrics: <http://localhost:8080/actuator/prometheus>
- Grafana: <http://localhost:3001> (`admin` / `admin`, 로컬 전용)

OpenAPI 스펙은 다음 명령으로 생성하며 저장소 루트의 `docs/openapi.json`에 기록됩니다.

```bash
./gradlew exportOpenApi
```

## 테스트

```bash
./gradlew test
```

Testcontainers가 MySQL 컨테이너를 띄우므로 Docker가 실행 중이어야 합니다.

## 기술 스택

- Java 25, Spring Boot 4
- Spring Batch — RSS 수집 배치
- Spring Data JPA, Flyway, MySQL 8.4 — FULLTEXT 검색
- Redis 7.4 — 조회수 중복 제한·집계
- springdoc-openapi — OpenAPI 스펙 export
- Gemini API — 한국어·영어 요약과 카테고리 분류
- Prometheus, Grafana, JUnit 5, MySQL Testcontainers
