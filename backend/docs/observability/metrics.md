# 백엔드 메트릭

백엔드는 Spring Boot Actuator와 Micrometer를 통해 Prometheus 형식의 메트릭을 노출한다.

## 엔드포인트

- Health check: `GET /actuator/health`
- Prometheus scrape target: `GET /actuator/prometheus`

현재 actuator 노출 범위는 `health,prometheus`로 고정한다. 운영에서 불필요한 actuator endpoint가 열리지 않도록, 새 endpoint가 필요하면 코드 리뷰를 거쳐 명시적으로 추가한다.

## 로컬 실행

로컬 개발용 compose는 백엔드 앱을 직접 호스트에서 실행한다는 전제다. `backend` 디렉터리에서 다음 명령을 실행한다.

```bash
docker compose -f docker-compose.local.yml up -d
```

그 다음 백엔드를 로컬 `8080` 포트로 실행한다.

- Prometheus: http://localhost:9090
- Grafana: http://localhost:3001

로컬 Prometheus는 `prometheus.local.yml`을 사용하며, Docker 컨테이너에서 호스트 머신의 백엔드를 보기 위해 `host.docker.internal:8080/actuator/prometheus`를 scrape한다. Grafana dashboard/provisioning은 `../infra/mono/grafana`를 재사용해서 로컬과 운영의 대시보드 구성을 맞춘다.

로컬 Grafana 기본 계정은 별도 환경변수가 없으면 `admin` / `admin`이다.

## 운영 실행

운영 설정은 프로젝트 루트의 `infra/mono` 아래에 둔다.

```text
infra/mono/
  docker-compose.yml
  prometheus/prometheus.yml
  grafana/
    provisioning/
    dashboards/
```

운영에서는 백엔드도 Docker Compose 서비스로 실행되므로 Prometheus가 `backend:8080/actuator/prometheus`를 scrape한다. Prometheus는 백엔드 health check에 의존하지 않고 먼저 실행되며, 백엔드가 unhealthy이면 target 상태를 `DOWN`으로 보여준다.

Grafana와 Prometheus는 서버 내부 주소에만 바인딩한다. Grafana는 `127.0.0.1:3001`, Prometheus는 `127.0.0.1:9090`으로 접근할 수 있으며, 외부에 직접 공개하지 않고 SSH 터널로 접속한다.

```bash
ssh -L 3001:127.0.0.1:3001 user@your-server
```

터널을 연 뒤 브라우저에서 http://localhost:3001 로 접속한다.

운영 secret/env 파일은 CI가 복사하지 않는다. 서버의 `/opt/everytldr` 아래에서 직접 관리한다. Grafana 설정도 `/opt/everytldr/.env.grafana` 파일에 둔다.

```text
.env.frontend
.env.backend
.env.db
.env.grafana
```

Grafana env 파일에는 최소한 다음 값을 둔다.

```env
GF_SECURITY_ADMIN_USER=admin
GF_SECURITY_ADMIN_PASSWORD=replace-with-long-random-password
```

`GF_USERS_ALLOW_SIGN_UP=false`는 운영 compose에 이미 있으므로 `.env.grafana`에 중복 작성하지 않는다. Grafana 비밀번호는 GitHub Secret이 아니라 운영 서버의 `.env.grafana`에서 관리한다.

## 주요 메트릭

- `everytldr_ingestor_articles_total`: 수집 article 저장 결과. `result` 태그로 `saved`, `invalid_skipped`, `existing_duplicate_skipped` 등을 구분한다.
- `everytldr_ingestor_sources_total`: source 처리 시도 수. `source_type`, `outcome` 태그를 가진다.
- `everytldr_ingestor_source_duration_seconds`: source 처리 시간. Prometheus에서는 timer가 `_seconds_count`, `_seconds_sum`, `_seconds_max` 같은 시계열로 노출된다.
- `everytldr_enricher_jobs_total`: enricher job 처리 결과. `status` 태그로 성공, 실패, 재시도 예약, skip 등을 구분한다.
- `everytldr_enricher_jobs_backlog`: 현재 enricher backlog. `state` 태그로 `pending`, `retry_scheduled`, `processing`을 구분한다.

PromQL 예시:

```promql
sum by (result) (increase(everytldr_ingestor_articles_total[1h]))
sum by (status) (increase(everytldr_enricher_jobs_total[1h]))
everytldr_enricher_jobs_backlog
sum by (method, uri, status) (rate(http_server_requests_seconds_count[5m]))
```

메트릭 태그에는 값 종류가 적고 안정적인 값만 사용한다. URL, article id, job id, source name, raw client address처럼 값이 계속 늘어나는 정보는 메트릭 태그로 쓰지 않고 로그에 남긴다.
