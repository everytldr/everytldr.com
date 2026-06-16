# 1. Scope

Persistence layer for the everytldr backend: entities, DB mapping, ID generation, and shared conventions. Reader: a coding agent with no prior context on this codebase. Out of scope: HTTP layer, ingestion/summarization logic, frontend.

# 2. Stack and module layout

## 2.1. Runtime

| Component | Choice |
|---|---|
| Java | 25 |
| Spring Boot | 4.0.6 |
| Persistence | Spring Data JPA over Hibernate 7.2.12 |
| RDBMS (prod / test) | MySQL 8.4 / MySQL 8.4 via Testcontainers (§ 6.) |
| Migration | Flyway (`spring-boot-starter-flyway`) |
| Build | Gradle Kotlin DSL |
| ID generator | Custom Snowflake (§ 8.) |

## 2.2. Module layout

Single Gradle project. Five Spring profiles realized as Java packages, not subprojects.

| Profile | Package root | Role |
|---|---|---|
| `common` | `com.everytldr.common` | Domain entities, repositories, snowflake, base classes |
| `api` | `com.everytldr.api` | HTTP endpoints, DTOs |
| `ingestor` | `com.everytldr.ingestor` | RSS polling, article ingestion |
| `enricher` | `com.everytldr.enricher` | LLM translation and summarization |
| `monolith` | inherits all | Single-process deployment |

Entities live under `com.everytldr.common.domain.<aggregate>` (package-by-feature). Repositories sit beside their entities; no top-level `repository/` folder.

## 2.3. Domain package layout

```
common.domain.support/      BaseEntity, SoftDeletableEntity, SnowflakeId,
                            SnowflakeIdGenerator, HibernateSnowflakeIdGenerator
common.domain.article/      Article, ArticleSummary, ArticleLike, ArticleComment, *Repository
common.domain.category/     Category, ArticleCategory, *Repository
common.domain.ingestion/    ArticleIngestionJob, IngestionState, *Repository
common.domain.source/       ArticleSource, SourceType, SourcePolicy, *Repository
common.domain.language/     SupportedLanguage
```

# 3. Database conventions

## 3.1. Character set

All tables: `utf8mb4 / utf8mb4_0900_ai_ci` (emoji and full Unicode in nicknames/comments).

## 3.2. Time

MySQL `DATETIME(6)` (UTC) ↔ Java `java.time.Instant`. `TIMESTAMP` is avoided to prevent implicit time-zone conversion.

## 3.3. Identifiers

Every entity uses a 64-bit application-generated Snowflake ID (§ 8.) stored as `BIGINT NOT NULL PRIMARY KEY` — no `AUTO_INCREMENT`, no sequences. IDs are monotonic per worker, so list ordering uses `ORDER BY id DESC`. **No `created_at` column exists**; creation time is a transient field on `BaseEntity` decoded from the ID (§ 4.1., § 8.6.).

## 3.4. Foreign keys

All FKs `ON DELETE RESTRICT, ON UPDATE RESTRICT`, except `article.source → article_source(name)` which is `ON UPDATE CASCADE` (publisher rename propagates; § 7.2.2.–§ 7.2.3.). Parent hard-deletion is therefore deliberate; routine deletion is soft (§ 3.5.). No `CascadeType.*`; no bidirectional `@OneToMany`.

## 3.5. Soft delete

`Article` and `ArticleComment` extend `SoftDeletableEntity`, inheriting `deleted_at DATETIME(6) NULL` (`NULL` = active). Filter: `@SQLRestriction("deleted_at IS NULL")` is declared on the mapped superclass and propagates to all subclasses at metamodel build (verified by `SoftDeleteTest`), so subclasses do not repeat it.

| Operation | Effect |
|---|---|
| `entity.softDelete(now)` + `save` | Sets `deleted_at` via dirty `UPDATE` |
| `find` / JPQL / Criteria SELECT | Hibernate appends `AND deleted_at IS NULL` |
| Lazy load of a soft-deleted target | Filter applies; target reads as absent |
| `repository.delete(entity)` | Real SQL `DELETE`; reserved for moderation hard-delete |

`@SoftDelete` is not used: it forbids `LAZY @ManyToOne` to soft-deletable targets (both entities are such targets), whereas `@SQLRestriction` filters at query time without that restriction. Moderation reads of deleted rows must bypass the filter via native SQL. Deleting a parent comment does not delete its replies; a child whose `parent` was soft-deleted loads no parent row (treated as "parent unavailable"), and the UI shows a placeholder.

## 3.6. Naming

| Element | Convention |
|---|---|
| Tables | `snake_case`, singular; auto-derived from entity name by `CamelCaseToUnderscoresNamingStrategy`. `@Table(name)` only to override |
| Columns | `snake_case`, auto-derived from field name. `@Column(name)` only to override |
| Java entities / fields | `PascalCase` singular / `camelCase` |
| Index / unique / FK names | Explicit: `idx_<table>_<cols>`, `uk_<table>_<cols>`, `fk_<table>_<referenced>` |
| `@JoinColumn(name)` | Always explicit, to anchor FK column names against field renames |

# 4. Persistence framework conventions

## 4.1. Base mapped superclasses

```
BaseEntity (@MappedSuperclass, @EntityListeners(AuditingEntityListener))
├── id        : Long     (@Id @SnowflakeId; § 8.)
├── updatedAt : Instant  (@LastModifiedDate)
└── createdAt : Instant  (@Transient; decoded from id via @PostLoad/@PostPersist; § 8.6.)
        ▲ extends
SoftDeletableEntity (@MappedSuperclass, @SQLRestriction("deleted_at IS NULL"))
└── deletedAt : Instant  (nullable; § 3.5.; methods softDelete(Instant), isSoftDeleted())
```

`createdAt` is not a column; a JPA callback fills it after `@PostLoad`/`@PostPersist`:

| State | `getCreatedAt()` |
|---|---|
| New, not persisted | `null` |
| After `persist`, before flush | `null` (ID assigned at flush; callback fires after `INSERT`) |
| After flush / loaded | decoded `Instant` |

Base class per entity: `SoftDeletableEntity` for `Article`, `ArticleComment`; `BaseEntity` for `ArticleSummary`, `ArticleLike`, `ArticleCategory`, `ArticleIngestionJob`, `Category`, `ArticleSource`.

## 4.2. Lombok policy

Allowed: `@Getter`, `@NoArgsConstructor(access = PROTECTED)` (JPA), `@Builder` / static factory. Forbidden: `@Setter`, `@EqualsAndHashCode` (write manually; § 4.6.), `@ToString` (lazy-load hazard), `@Data`.

## 4.3. Validation policy

JPA constraints (`nullable`, `length`, `unique`) on entity columns. Bean Validation (`@NotNull`, `@Size`, …) belongs on HTTP DTOs, not entities.

## 4.4. Auditing

`@EnableJpaAuditing` on `com.everytldr.common.CommonConfig`. `@LastModifiedDate` fills `updatedAt` on INSERT and UPDATE (so at first persist `updatedAt` = persist instant). `@CreatedBy`/`@LastModifiedBy` unused (anonymous service).

## 4.5. Fetch and association rules

| Rule | Reason |
|---|---|
| All `@ManyToOne`/`@OneToOne` are `LAZY` | Avoid hidden N+1 |
| No bidirectional mappings | Avoid cycles / cascade accidents |
| No `cascade = *` | Hard deletion must be explicit (§ 3.4.) |
| No `@ManyToMany` | Replaced by an explicit join entity (§ 7.2.8.) |

## 4.6. Equality

`equals`/`hashCode` use `id` plus the proxy-unwrapped class (`Hibernate.getClass(...)`): equal iff unwrapped runtime classes match and both `id`s are non-null and equal (a transient `id == null` entity equals only itself by reference). `hashCode` is the unwrapped class's constant hashCode, stable across the transient→managed transition.

# 5. Schema migration

## 5.1. Bootstrap

1. Define entities per § 3.–§ 4.
2. Run once with `ddl-auto=update` on an empty schema (Hibernate creates tables).
3. Dump via `mysqldump --no-data` to `src/main/resources/db/migration/V1__init.sql`.
4. Switch `ddl-auto=validate`.

Already executed. In the committed `V1__init.sql`, tables were reordered to topological FK order and `mysqldump` session pragmas stripped so it loads without `FOREIGN_KEY_CHECKS=0`. `application.yaml` exposes `JPA_DDL_AUTO` (default `validate`) and `FLYWAY_ENABLED` (default `true`) to re-baseline (drop schema → run `JPA_DDL_AUTO=update FLYWAY_ENABLED=false` → dump → reset).

## 5.2. Steady state

| Change | Procedure |
|---|---|
| Schema change | New `V{n}__{description}.sql` |
| Reference-data seed | New `V{n}__seed_{table}.sql` |
| Entity-only, no DDL | No migration |

`ddl-auto` stays `validate`: Hibernate refuses to start if mappings drift from the live schema.

# 6. Test database strategy

No H2. Tests use a real MySQL 8 Testcontainers container (one per JVM, reused, per-test rollback) for dialect parity with production.

| Gradle dependency | Config |
|---|---|
| `spring-boot-testcontainers` | `testImplementation` |
| `org.testcontainers:mysql`, `:junit-jupiter` | `testImplementation` |
| `com.h2database:h2`, `spring-boot-h2console` | removed |

# 7. Entity catalog

## 7.1. Implementation order

Topological FK order: `Category` → `ArticleSource` → `Article` → `ArticleIngestionJob` → `ArticleSummary` → `ArticleLike` → `ArticleComment` → `ArticleCategory`.

## 7.2. Per-entity specifications

Inherited columns are omitted: every table has `id BIGINT NOT NULL PRIMARY KEY` and `updated_at DATETIME(6) NOT NULL` (`BaseEntity`); `SoftDeletableEntity` tables add `deleted_at DATETIME(6) NULL` (§ 3.5.).

### 7.2.1. `category`

| Column | Type | Constraint | Notes |
|---|---|---|---|
| `slug` | `VARCHAR(50)` | `NOT NULL UNIQUE` | URL-safe id; hierarchy encoded with `-`, e.g. `sport-football-epl-arsenal` |

Display labels and hierarchy are resolved client-side from `slug`; no `name`/`sort_order` column. The taxonomy is a product-defined multi-level hierarchy from top topics (`world`, `politics`, `society`, `economy`, `environment`, `technology`, `science`, `health`, `education`, `culture`, `sport`, …) down to specific entities (e.g. individual sport teams), seeded as Flyway reference data. Rows must exist before enrichment so the enricher can pick an allowed category.

### 7.2.2. `article_source`

| Column | Type | Constraint | Notes |
|---|---|---|---|
| `name` | `VARCHAR(100)` | `NOT NULL UNIQUE` (`uk_article_source_name`) | Display name; referenced by `article.source` FK (§ 7.2.3.) |
| `policy` | `JSON` | `NOT NULL` | Serialized `SourcePolicy` (`@JdbcTypeCode(SqlTypes.JSON)`); see below |
| `is_active` | `BOOLEAN` | `NOT NULL DEFAULT TRUE`, indexed | Inactive sources skipped |
| `language` | `VARCHAR(10)` | `NOT NULL` | BCP-47 lowercase; articles inherit it |
| `source_type` | `VARCHAR(32)` | `NOT NULL` | `SourceType` enum (STRING + `VARCHAR` JDBC type). Discriminates the collection channel; the ingestor dispatches to a matching `SourceClient` via a registry. Extension point for multiple channels (RSS, API, …); `RSS` is the only value implemented so far |

`SourcePolicy.CrawlingPolicy` JSON keys:

| Key | Required | Meaning |
|---|---|---|
| `feed_urls` | yes, non-empty | RSS feed URLs to poll |
| `hosts` | yes, non-empty | Hostname allowlist for page fetches |
| `content_selectors` | yes, non-empty | CSS selectors for the article body |
| `thumbnail_selectors` | no | CSS selectors for the thumbnail; defaults to empty |

Seed rows are Flyway reference data with explicit fixed Snowflake-format IDs (SQL seeds bypass `@SnowflakeId`): project epoch/layout (§ 8.), `workerId = 1023` reserved for reference data. Runtime processes use `APP_WORKER_ID` in `0..1022` so IDs cannot collide.

### 7.2.3. `article`

Base: `SoftDeletableEntity`.

| Column | Type | Constraint | Notes |
|---|---|---|---|
| `content_url` | `VARCHAR(1000)` | `NOT NULL` | Original article URL |
| `source` | `VARCHAR(100)` | `NOT NULL`, FK → `article_source(name)` | Publisher name; FK `fk_article_source_name`, `ON UPDATE CASCADE` (§ 3.4.) |
| `thumbnail_url` | `VARCHAR(1000)` | `NULL` | |
| `language` | `VARCHAR(10)` | `NOT NULL` | BCP-47 lowercase |
| `published_at` | `DATETIME(6)` | `NOT NULL`, indexed | Backs `(published_at DESC, id DESC)` ordering |

The original (untranslated) title is not stored (§ 9. Q3).

### 7.2.4. `article_ingestion_job`

Base: `BaseEntity`. 1:1 with `article`.

| Column | Type | Constraint | Notes |
|---|---|---|---|
| `article_id` | `BIGINT` | `NOT NULL UNIQUE`, FK → `article(id)` | One job per article |
| `url_hash` | `BINARY(32)` | `NOT NULL UNIQUE` | SHA-256 of `content_url` |
| `state` | `VARCHAR(32)` | `NOT NULL` | `IngestionState` enum |
| `attempt_count` | `INT` | `NOT NULL` | Incremented on each claim |
| `attempt_started_at` | `DATETIME(6)` | `NULL` | Set on claim; `NULL` while not `PROCESSING`; backs stale reclaim |
| `next_attempt_at` | `DATETIME(6)` | `NULL` | Earliest retry instant while `RETRY_SCHEDULED` |
| `last_error_message` | `VARCHAR(1000)` | `NULL` | Last failure message, truncated to 1000 |

Indexes: `(state, next_attempt_at)` selects due jobs; `(state, attempt_started_at)` finds stale `PROCESSING` jobs. `IngestionState`: `PENDING`, `PROCESSING`, `SUCCEEDED`, `FAILED`, `RETRY_SCHEDULED`; transitions, claiming, reclaim, and retry scheduling are enforced in `ArticleIngestionJob`, not the DB. `state` uses `@Enumerated(STRING)` + `@JdbcTypeCode(SqlTypes.VARCHAR)` to force plain `VARCHAR(32)` over Hibernate's default native `ENUM`; a `CHECK` constraint still lists valid values (adding one needs a DROP/ADD CONSTRAINT migration).

Flow: ingestor saves article metadata + `ArticleIngestionJob(PENDING)` (no category); enricher later writes summaries, selects one category, and marks the job by outcome. Dedupe: compute `url_hash = SHA-256(content_url)`; if a job row exists, skip; else in one transaction insert `article` then the job — concurrent inserts collide on the `url_hash` UNIQUE index and the loser rolls back.

### 7.2.5. `article_summary`

| Column | Type | Constraint | Notes |
|---|---|---|---|
| `article_id` | `BIGINT` | `NOT NULL`, FK → `article(id)` | |
| `language` | `VARCHAR(10)` | `NOT NULL` | BCP-47 lowercase |
| `title` | `VARCHAR(500)` | `NOT NULL` | Translated title |
| `content` | `TEXT` | `NOT NULL` | LLM summary |

Composite UNIQUE `(article_id, language)`. Produced by the enricher; the same successful flow also creates the `ArticleCategory` row. If summary or category selection fails, the job is not `SUCCEEDED`.

### 7.2.6. `article_like`

| Column | Type | Constraint | Notes |
|---|---|---|---|
| `article_id` | `BIGINT` | `NOT NULL`, FK → `article(id)` | |
| `ip_hash` | `CHAR(64)` | `NOT NULL` | HMAC-SHA256(pepper, IP), hex |
| `is_active` | `BOOLEAN` | `NOT NULL DEFAULT TRUE` | `FALSE` = unliked |

Composite UNIQUE `(article_id, ip_hash)`; indexed `(article_id, is_active)`. Toggling reuses the row (only `is_active`/`updatedAt` change). Active count: `COUNT(*) WHERE article_id = ? AND is_active = TRUE`.

### 7.2.7. `article_comment`

Base: `SoftDeletableEntity`.

| Column | Type | Constraint | Notes |
|---|---|---|---|
| `article_id` | `BIGINT` | `NOT NULL`, FK → `article(id)` | |
| `parent_id` | `BIGINT` | `NULL`, FK → `article_comment(id)` | `NULL` = top-level |
| `nickname` | `VARCHAR(50)` | `NOT NULL` | Display only |
| `password_hash` | `CHAR(60)` | `NOT NULL` | bcrypt; gates self-delete |
| `ip_hash` | `CHAR(64)` | `NOT NULL` | Stable hash (§ 7.2.6.), never displayed |
| `masked_ip` | `VARCHAR(32)` | `NOT NULL` | Partially masked IP for display; derived at write time |
| `content` | `TEXT` | `NOT NULL` | Body; DTO-level ≤ 5000 |

Indexed `(article_id, id)` for chronological loading. Soft-delete: § 3.5.

### 7.2.8. `article_category`

Explicit join entity (not `@ManyToMany`); stores the category chosen during enrichment.

| Column | Type | Constraint |
|---|---|---|
| `article_id` | `BIGINT` | `NOT NULL`, FK → `article(id)` |
| `category_id` | `BIGINT` | `NOT NULL`, FK → `category(id)` |

Composite UNIQUE `(article_id, category_id)`. Exactly one category per article is a business invariant enforced in the enricher (and tests); the schema only blocks duplicate pairs, not multiple distinct categories.

## 7.3. Excluded

`article_views` (view-count dedupe from the PRD) is not implemented; deferred.

# 8. Snowflake generator

## 8.1. Bit layout

64-bit signed `long`, sign bit always 0 (IDs stay positive). Order MSB→LSB: `sign(1) | timestamp(41) | worker(10) | sequence(12)`, so IDs are monotonically non-decreasing in time per worker.

| Field | Bits | Range |
|---|---|---|
| timestamp (ms since epoch § 8.2.) | 41 | ~69 years |
| worker ID | 10 | 0..1023 |
| sequence (per ms) | 12 | 0..4095 |

## 8.2. Epoch

`2026-01-01T00:00:00Z`, a constant. Changing it invalidates all IDs and is forbidden after the first production write.

## 8.3. Worker ID configuration

Resolution: Spring property `everytldr.snowflake.worker-id` → env `APP_WORKER_ID` → default `0`. Operators must assign distinct worker IDs to concurrent processes; duplicates can mint identical IDs and violate the `BIGINT PRIMARY KEY`.

## 8.4. Clock skew

`generateId()` tracks the last-issued millisecond:

| Condition | Action |
|---|---|
| `now > last` | Reset sequence to 0; emit |
| `now == last`, seq < 4095 | Increment; emit |
| `now == last`, seq == 4095 | Busy-wait to next ms; emit |
| `now < last`, gap < 1000 ms | Busy-wait until `now >= last`; emit |
| `now < last`, gap ≥ 1000 ms | Throw `IllegalStateException` |

## 8.5. Hibernate integration

| Type | Role |
|---|---|
| `SnowflakeIdGenerator` (`@Component`) | Holds `Clock`, worker ID, last-ms/sequence state. `synchronized long generateId()` + static decoder (§ 8.6.) |
| `HibernateSnowflakeIdGenerator` (`BeforeExecutionGenerator`) | Resolves the bean via `ManagedBeanRegistry`, delegates `generate(...)`; `getEventTypes()` = `INSERT_ONLY` |
| `@SnowflakeId` | Field meta-annotation (`@IdGeneratorType(HibernateSnowflakeIdGenerator.class)`), replacing deprecated `@GenericGenerator` |

Each PK is `@Id @SnowflakeId private Long id;`. Entities never set `id`; Hibernate generates it before `INSERT`.

## 8.6. Timestamp decoding

```java
public static Instant extractTimestamp(long id) {
    return Instant.ofEpochMilli(EPOCH_MS + (id >>> TIMESTAMP_SHIFT));
}
```

`TIMESTAMP_SHIFT = WORKER_BITS + SEQUENCE_BITS = 22`. Unsigned `>>>` documents the always-0 sign bit invariant.

# 9. Resolved decisions

| ID | Question | Decision | Where |
|---|---|---|---|
| Q1 | Generalize `rss_source` to `article_source`? | Yes; provider-specific channel with `language` and `source_type`. | § 7.2.2. |
| Q2 | How is the taxonomy seeded? | Flyway reference-data seeds, fixed Snowflake IDs (`workerId = 1023`). | § 7.2.1. |
| Q3 | Store original article title? | No; identify via `content_url` + active-language `article_summary.title`. | § 7.2.3. |
| Q4 | Add retry/observability fields to the job? | Added: `attempt_count`, `attempt_started_at`, `next_attempt_at`, `last_error_message` + index. | § 7.2.4. |
