# everytldr Backend

Spring Boot 서버입니다. 하나의 Gradle 프로젝트를 Spring 프로필로 역할을 나눠 실행합니다 — `api`(REST API), `ingestor`(RSS에서 새 기사 수집), `enricher`(본문 크롤링과 AI 요약·카테고리 분류). `monolith` 프로필은 셋을 한 프로세스로 묶습니다.

## 실행

Java 25가 필요합니다.

```bash
# MySQL (+ Prometheus/Grafana) 로컬 스택
docker compose -f docker-compose.local.yml up -d

# monolith 프로필로 실행
SPRING_PROFILES_ACTIVE=monolith ./gradlew bootRun
```

첫 부팅 때 Flyway가 스키마를 만들고, DB 접속 기본값은 `docker-compose.local.yml`의 계정과 맞춰져 있어 따로 설정할 게 없습니다. API는 http://localhost:8080 에서 뜨고, Swagger UI는 `/swagger-ui.html`에서 볼 수 있습니다.

수집과 요약 스케줄러는 기본으로 꺼져 있습니다. 실제 수집부터 요약까지 돌려보려면 백엔드 루트의 `.env`에 다음을 넣어주세요.

```
INGESTION_ENABLED=true
ENRICHER_PROCESSING_ENABLED=true
ENRICHER_AI_GEMINI_ENABLED=true
GEMINI_API_KEY=...
```

## 테스트

```bash
./gradlew test
```

Testcontainers가 MySQL 컨테이너를 띄우므로 Docker가 실행 중이어야 합니다.

## 기술 스택

- Java 25, Spring Boot 4
- Spring Batch — RSS 수집 배치
- Spring Data JPA, Flyway
- MySQL 8.4 — FULLTEXT 검색
- springdoc-openapi — `docs/openapi.json` 스펙 export
- Gemini API — 번역·요약·카테고리 분류
- JUnit 5, MySQL Testcontainers
