# 1. Scope

This document specifies the persistence layer for the everytldr backend: which entities exist, how they map to the database, how identifiers are generated, and which conventions every entity follows. Reader: an autonomous coding agent with no prior context on this codebase.

Out of scope: HTTP layer, ingestion business logic, summarization pipeline, frontend.

# 2. Stack and module layout

## 2.1. Runtime

| Component | Version or choice |
|---|---|
| Java toolchain | 25 |
| Spring Boot | 4.0.6 |
| Persistence | Spring Data JPA over Hibernate 7.2.12 |
| Production RDBMS | MySQL 8.4 |
| Test RDBMS | MySQL 8.4 via Testcontainers (§ 6.) |
| Migration tool | Flyway, registered via `spring-boot-starter-flyway` |
| Build tool | Gradle Kotlin DSL |
| ID generator | Custom Snowflake (§ 8.) |
| Lombok policy | § 4.2. |

## 2.2. Module layout

The backend is a single Gradle project. The five Spring profiles are realized as Java packages, not as Gradle subprojects.

| Profile | Package root | Role |
|---|---|---|
| `common` | `org.tldrtimes.common` | Shared code: domain entities, repositories, snowflake, base classes |
| `api` | `org.tldrtimes.api` | HTTP endpoints, DTOs |
| `ingestor` | `org.tldrtimes.ingestor` | RSS polling, article ingestion |
| `enricher` | `org.tldrtimes.enricher` | LLM translation and summarization |
| `monolith` | inherits all | Single-process deployment |

All persistence entities live under `org.tldrtimes.common.domain.<aggregate>`. This is package-by-feature (§ 10.).

## 2.3. Domain package layout

```
common.domain.support/      BaseEntity, SoftDeletableEntity,
                            SnowflakeId (meta-annotation),
                            SnowflakeIdGenerator,
                            HibernateSnowflakeIdGenerator
common.domain.article/      Article, ArticleSummary, ArticleLike, ArticleComment, *Repository
common.domain.category/     Category, ArticleCategory, *Repository
common.domain.ingestion/    ArticleIngestionJob, IngestionState, *Repository
common.domain.source/       ArticleSource, SourceType, *Repository
```

Repositories sit beside their entities in the same package. There is no top-level `repository/` folder.

# 3. Database conventions

## 3.1. Character set

All tables: `CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci`. Required for emoji and full Unicode in nicknames and comments.

## 3.2. Time

| Surface | Type |
|---|---|
| MySQL columns | `DATETIME(6)` storing UTC values |
| Java fields | `java.time.Instant` |

`TIMESTAMP` is not used; `DATETIME` avoids implicit time zone conversion at the column level.

## 3.3. Identifiers

Every entity uses a 64-bit Snowflake ID generated in the application (§ 8.). Stored as `BIGINT NOT NULL PRIMARY KEY`. No `AUTO_INCREMENT`. No table sequences.

Consequence: row insertion order is monotonic in `id` per worker. List ordering uses `ORDER BY id DESC` instead of `ORDER BY created_at DESC`. **There is no `created_at` column on any table**; creation time is exposed in Java via a derived transient field on `BaseEntity` (§ 4.1.) backed by the decoder in § 8.6.

## 3.4. Foreign keys

All foreign keys: `ON DELETE RESTRICT, ON UPDATE RESTRICT`. Hard deletion of a parent row is therefore a deliberate multi-statement operation; routine deletion uses soft delete (§ 3.5.).

JPA-side cascade attributes (`CascadeType.*`) are not used. Bidirectional `@OneToMany` mappings are not declared.

## 3.5. Soft delete

`Article` and `ArticleComment` are soft-deletable. They inherit a `deleted_at DATETIME(6) NULL` column from `SoftDeletableEntity` (§ 4.1.) — `NULL` means active, a non-`NULL` timestamp means deleted at that instant.

Filter mechanism: Hibernate `@SQLRestriction("deleted_at IS NULL")` placed **on `SoftDeletableEntity` itself** (the mapped superclass). It propagates to every concrete subclass at metamodel build time — verified empirically against Hibernate 7 by `SoftDeleteTest`. Concrete subclasses (`Article`, `ArticleComment`) therefore do **not** repeat the annotation. If the test ever fails on a future Hibernate upgrade, restore the per-subclass annotation as a workaround and reopen this question.

| Operation | Effect |
|---|---|
| `entity.softDelete(Instant.now())` + `repository.save(entity)` | Sets `deleted_at`, dirty checking emits `UPDATE ... SET deleted_at = ?` |
| `entityManager.find` / JPQL / Criteria SELECTs | Hibernate appends `AND deleted_at IS NULL` to every read against the entity |
| Lazy association loads of a soft-deletable target | Same filter applies; the target row reads as absent |
| `repository.delete(entity)` | Issues a real SQL `DELETE` (not soft-delete). Reserved for hard-delete by moderation, gated by `ON DELETE RESTRICT` (§ 3.4.) |

Why not Hibernate `@SoftDelete`: that annotation is incompatible with our LAZY fetch policy. Hibernate refuses to map a `LAZY @ManyToOne` whose target is `@SoftDelete`-marked (`UnsupportedMappingException` at `ToOneAttributeMapping.java:422`), because the proxy cannot determine soft-delete state without loading the row. Our `Article` is the target of five LAZY associations (§ 7.2.4.–§ 7.2.8.) and `ArticleComment` is the target of one self-referential LAZY association (§ 7.2.7.); soft-deleting them via `@SoftDelete` would force EAGER fetching and break § 4.5. `@SQLRestriction` does not have this restriction because it runs at query time, not at proxy-resolution time.

Moderation tooling that needs deleted rows must use native SQL or a dedicated repository method that bypasses the ORM filter (e.g., `@Query(nativeQuery = true)`).

Comment thread semantics: deleting a parent comment does not soft-delete its children. The UI replaces the deleted parent's body and nickname with a placeholder while children remain visible. Implementation detail: when a child comment lazy-loads its `parent` reference and the parent was soft-deleted, the load returns no row; the application code treats this as "parent unavailable" rather than an error.

## 3.6. Naming

| Element | Convention |
|---|---|
| Tables | `snake_case`, singular (`article_summary`). Derived from the entity class name by `org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy`, anchored in `application.yaml` (`spring.jpa.hibernate.naming.physical-strategy`). `@Table(name = "...")` is omitted unless overriding the auto-derived name. |
| Columns | `snake_case`. Same naming strategy converts `camelCase` Java field names to `snake_case` columns. `@Column(name = "...")` is omitted unless overriding the auto-derived name. |
| Java entities | `PascalCase`, singular (`ArticleSummary`). |
| Java fields | `camelCase`. |
| Indexes / unique constraints / FK names | Explicitly named via `@Index(name = ...)`, `@UniqueConstraint(name = ...)`, `@ForeignKey(name = ...)`. Pattern: `idx_<table>_<columns>`, `uk_<table>_<columns>`, `fk_<table>_<referenced>`. |
| `@JoinColumn(name = ...)` | Always explicit. Anchors FK column names against silent renames if the Java association field is later refactored (e.g., `Article article` → `Article subject`). |

# 4. Persistence framework conventions

## 4.1. Base mapped superclasses

```
BaseEntity (@MappedSuperclass, @EntityListeners(AuditingEntityListener))
├── id        : Long     (@Id @SnowflakeId; § 8.)
├── updatedAt : Instant  (@LastModifiedDate)
└── createdAt : Instant  (@Transient; derived from id via
                          @PostLoad @PostPersist callback;
                          decoder in § 8.6.)
        ▲
        │ extends
SoftDeletableEntity (@MappedSuperclass, @SQLRestriction("deleted_at IS NULL"))
└── deletedAt : Instant  (nullable; null = active, set = deleted; § 3.5.)
    Domain methods: softDelete(Instant), isSoftDeleted()
```

`createdAt` is **not** a database column. It is populated by a private JPA callback that runs after `@PostLoad` (read path) and `@PostPersist` (write path). Property semantics:

| State | `getCreatedAt()` returns |
|---|---|
| Newly constructed (`new`), not persisted | `null` |
| After `entityManager.persist(...)`, before flush | `null` (blank window: ID is assigned at flush time by the `BeforeExecutionGenerator`; the callback fires only after `INSERT` executes) |
| After flush (`@PostPersist` fired) | decoded `Instant` |
| Loaded via repository / query (`@PostLoad` fired) | decoded `Instant` |

The blank window between `persist` and flush is accepted as a design tradeoff against the alternative of recomputing per call.

Allocation per entity:

| Entity | Base class |
|---|---|
| `Article` | `SoftDeletableEntity` |
| `ArticleComment` | `SoftDeletableEntity` |
| `ArticleSummary` | `BaseEntity` |
| `ArticleLike` | `BaseEntity` |
| `ArticleCategory` | `BaseEntity` |
| `ArticleIngestionJob` | `BaseEntity` |
| `Category` | `BaseEntity` |
| `ArticleSource` | `BaseEntity` |

## 4.2. Lombok policy

| Annotation | Allowed on entities |
|---|---|
| `@Getter` | Yes |
| `@Setter` | No |
| `@NoArgsConstructor(access = PROTECTED)` | Yes (JPA requires) |
| `@Builder` or static factory | Yes |
| `@EqualsAndHashCode` | No (write manually, ID-based, null-safe; § 4.6.) |
| `@ToString` | No (lazy-loading hazard) |
| `@Data` | No |

## 4.3. Validation policy

JPA constraints (`nullable`, `length`, `unique`) on entity columns. Bean Validation (`@NotNull`, `@Size`, `@Pattern`) belongs on DTOs at HTTP boundaries, not on entities.

## 4.4. Auditing

`@EnableJpaAuditing` is declared on `org.tldrtimes.common.CommonConfig`. `@LastModifiedDate` populates `updatedAt` (§ 4.1.) on both INSERT and UPDATE; therefore at first persist, `updatedAt` equals the persist instant. `@CreatedBy` and `@LastModifiedBy` are not used (anonymous service).

## 4.5. Fetch and association rules

| Rule | Reason |
|---|---|
| All `@ManyToOne` and `@OneToOne` declared `fetch = LAZY` | Avoid hidden N+1 |
| No bidirectional mappings | Avoid cycles and cascade accidents |
| No `cascade = *` | Hard deletion must be explicit (§ 3.4.) |
| `@ManyToMany` not used | Replaced by an explicit join entity (§ 7.2.8.) |

## 4.6. Equality

Entity `equals` and `hashCode` are based on `id` and the proxy-safe class returned by `org.hibernate.Hibernate.getClass(...)`:

- `equals` returns `true` iff the runtime classes (after proxy unwrap) are identical and both `id`s are non-null and equal.
- A transient entity (`id == null`) is therefore equal only to itself by reference.
- `hashCode` returns the proxy-unwrapped class's `hashCode` (constant per concrete entity type), which keeps the contract stable across the transient-to-managed transition.

# 5. Schema migration

## 5.1. Bootstrap

1. Define all entities per § 3.–§ 4.
2. Run the application once with `spring.jpa.hibernate.ddl-auto=update` against an empty MySQL schema. Hibernate creates the initial tables.
3. Dump the resulting schema with `mysqldump --no-data` and place it as `src/main/resources/db/migration/V1__init.sql`.
4. Switch `spring.jpa.hibernate.ddl-auto=validate`.

The procedure has been executed. The committed output is `backend/src/main/resources/db/migration/V1__init.sql`. After the dump, tables were manually reordered to topological foreign-key order (`category` → `rss_source` → `article` → `article_ingestion_job` → `article_summary` → `article_like` → `article_comment` → `article_category`) so the migration loads without `FOREIGN_KEY_CHECKS=0`, and the `mysqldump` session pragmas (`/*!40101 ... */` etc.) were removed.

`application.yaml` exposes `JPA_DDL_AUTO` (default `validate`) and `FLYWAY_ENABLED` (default `true`) environment variables to support re-running the bootstrap procedure later. To re-baseline: drop the schema, run with `JPA_DDL_AUTO=update FLYWAY_ENABLED=false`, dump, reset both variables to defaults.

## 5.2. Steady state

| Change type | Procedure |
|---|---|
| Schema change | New `V{n}__{description}.sql` Flyway migration |
| Reference data seed | New `V{n}__seed_{table}.sql` Flyway migration |
| Entity-only change with no DDL impact | No migration |

After § 5.1. step 4, `ddl-auto` remains `validate`. Hibernate refuses to start if entity mappings drift from the live schema.

# 6. Test database strategy

H2 is removed from the build. Test code uses a real MySQL 8 container managed by Testcontainers. One container per JVM, reused across test classes, with per-test transaction rollback for isolation.

| Gradle dependency | Configuration |
|---|---|
| `org.springframework.boot:spring-boot-testcontainers` | `testImplementation` |
| `org.testcontainers:mysql` | `testImplementation` |
| `org.testcontainers:junit-jupiter` | `testImplementation` |
| `com.h2database:h2` | removed |
| `org.springframework.boot:spring-boot-h2console` | removed |

Rationale: dialect parity with production. SQL valid on H2 but invalid on MySQL is rejected at test time.

# 7. Entity catalog

## 7.1. Implementation order

Order is the topological sort of foreign-key dependency. Each step compiles only after the previous step.

1. § 7.2.1. `Category` (no FKs)
2. § 7.2.2. `ArticleSource` (no FKs)
3. § 7.2.3. `Article` (no FKs)
4. § 7.2.4. `ArticleIngestionJob` (FK → `Article`)
5. § 7.2.5. `ArticleSummary` (FK → `Article`)
6. § 7.2.6. `ArticleLike` (FK → `Article`)
7. § 7.2.7. `ArticleComment` (FK → `Article`, self-FK)
8. § 7.2.8. `ArticleCategory` (FK → `Article`, FK → `Category`)

## 7.2. Per-entity specifications

Inherited columns are omitted from each table; refer to § 4.1. Concretely:
- Every table inherits `id BIGINT NOT NULL PRIMARY KEY` and `updated_at DATETIME(6) NOT NULL` from `BaseEntity`.
- Tables backed by `SoftDeletableEntity` additionally carry `deleted_at DATETIME(6) NULL` (§ 3.5.).

### 7.2.1. `category`

| Column | Type | Constraint | Notes |
|---|---|---|---|
| `slug` | `VARCHAR(50)` | `NOT NULL UNIQUE` | URL-safe identifier, e.g. `football` |
| `sort_order` | `INT` | `NOT NULL DEFAULT 0` | Display order; lower first |

Display labels (Korean, English, …) are resolved client-side from `slug` via i18n resources; no `name` column.

### 7.2.2. `article_source`

| Column | Type | Constraint | Notes |
|---|---|---|---|
| `name` | `VARCHAR(100)` | `NOT NULL` | Display name, e.g. `BBC Sport` |
| `url` | `VARCHAR(500)` | `NOT NULL UNIQUE` | Provider locator. For RSS this is the feed URL; for Guardian API this is an API-key-free locator such as a section/search URL. |
| `is_active` | `BOOLEAN` | `NOT NULL DEFAULT TRUE`, indexed | Inactive sources skipped by ingestor |
| `language` | `VARCHAR(10)` | `NOT NULL` | BCP-47 lowercase, e.g. `en`. Articles emitted from this source inherit this value. |
| `source_type` | `VARCHAR(32)` | `NOT NULL` | Java `SourceType` enum, mapped `EnumType.STRING`; values include `GUARDIAN_API` and `RSS`. |

### 7.2.3. `article`

Base: `SoftDeletableEntity`.

| Column | Type | Constraint | Notes |
|---|---|---|---|
| `source_url` | `VARCHAR(1000)` | `NOT NULL` | Original article URL |
| `source` | `VARCHAR(100)` | `NOT NULL` | Publisher display name |
| `thumbnail_url` | `VARCHAR(1000)` | `NULL` | |
| `language` | `VARCHAR(10)` | `NOT NULL` | BCP-47 lowercase, e.g. `en` |
| `published_at` | `DATETIME(6)` | `NOT NULL`, indexed | Backs `(published_at DESC, id DESC)` list ordering |

The original (untranslated) article title is not stored. See § 9. Q3.

### 7.2.4. `article_ingestion_job`

Base: `BaseEntity`. 1:1 with `article`.

| Column | Type | Constraint | Notes |
|---|---|---|---|
| `article_id` | `BIGINT` | `NOT NULL UNIQUE`, FK → `article(id)` | One job per article |
| `url_hash` | `BINARY(32)` | `NOT NULL UNIQUE` | SHA-256 of `article.source_url` |
| `state` | `VARCHAR(32)` | `NOT NULL` | Java `IngestionState` enum, mapped `EnumType.STRING` |

`IngestionState` values: `PENDING`, `PROCESSING`, `SUCCEEDED`, `FAILED`, `RETRY_SCHEDULED`. Allowed transitions are enforced in domain code, not in the database.

The Java field is annotated `@Enumerated(EnumType.STRING)` plus `@JdbcTypeCode(SqlTypes.VARCHAR)`. Hibernate 6 and later default `@Enumerated(STRING)` to native MySQL `ENUM(...)`; the explicit `VARCHAR` JDBC type code suppresses that mapping and produces a plain `VARCHAR(32)`. Hibernate still emits a `CHECK` constraint named `article_ingestion_job_chk_1` enumerating the valid string values. The constraint is accepted: adding an enum value requires a `DROP CONSTRAINT` / `ADD CONSTRAINT` migration, which is lighter than altering a native `ENUM` column type. The `@UniqueConstraint` annotations on the entity's `@Table` give the two unique indexes explicit names (`uk_article_ingestion_job_article`, `uk_article_ingestion_job_url_hash`) instead of Hibernate's auto-generated hash names.

Dedupe flow:

1. Compute `url_hash = SHA-256(source_url)`.
2. Query `article_ingestion_job WHERE url_hash = ?`. If found, skip.
3. In one transaction: insert `article`, then insert `article_ingestion_job` with `state = PENDING`. Concurrent inserts collide on the `url_hash` UNIQUE index; the loser's transaction rolls back.

### 7.2.5. `article_summary`

| Column | Type | Constraint | Notes |
|---|---|---|---|
| `article_id` | `BIGINT` | `NOT NULL`, FK → `article(id)` | |
| `language` | `VARCHAR(10)` | `NOT NULL` | BCP-47 lowercase |
| `title` | `VARCHAR(500)` | `NOT NULL` | Translated title |
| `content` | `TEXT` | `NOT NULL` | LLM summary; no DB length cap |

Composite UNIQUE: `(article_id, language)`.

### 7.2.6. `article_like`

| Column | Type | Constraint | Notes |
|---|---|---|---|
| `article_id` | `BIGINT` | `NOT NULL`, FK → `article(id)` | |
| `ip_hash` | `CHAR(64)` | `NOT NULL` | HMAC-SHA256(pepper, IP), hex-encoded |
| `is_active` | `BOOLEAN` | `NOT NULL DEFAULT TRUE` | `FALSE` = unliked |

Composite UNIQUE: `(article_id, ip_hash)`. Indexed: `(article_id, is_active)`. Toggling reuses the row; only `is_active` and `updatedAt` change. Active count: `COUNT(*) WHERE article_id = ? AND is_active = TRUE`.

### 7.2.7. `article_comment`

Base: `SoftDeletableEntity`.

| Column | Type | Constraint | Notes |
|---|---|---|---|
| `article_id` | `BIGINT` | `NOT NULL`, FK → `article(id)` | |
| `parent_id` | `BIGINT` | `NULL`, FK → `article_comment(id)` | `NULL` = top-level |
| `nickname` | `VARCHAR(50)` | `NOT NULL` | Display only |
| `password_hash` | `CHAR(60)` | `NOT NULL` | bcrypt; gates self-delete |
| `ip_hash` | `CHAR(64)` | `NOT NULL` | Per § 7.2.6. |
| `content` | `TEXT` | `NOT NULL` | Body |

Indexed: `(article_id, id)` for chronological loading. Self-referential FK uses `parent_id`. DTO-level constraint: `content` length ≤ 2000.

Soft-delete behavior: see § 3.5.

### 7.2.8. `article_category`

Explicit join entity (not `@ManyToMany`).

| Column | Type | Constraint | Notes |
|---|---|---|---|
| `article_id` | `BIGINT` | `NOT NULL`, FK → `article(id)` | |
| `category_id` | `BIGINT` | `NOT NULL`, FK → `category(id)` | |

Composite UNIQUE: `(article_id, category_id)`.

## 7.3. Excluded

`article_views` from the source ERD is not implemented in this round. The PRD requirement that repeat views from the same reader do not inflate the view count is deferred to a follow-up design.

# 8. Snowflake generator

## 8.1. Bit layout

64-bit signed `long`. Most significant bit unused (always 0) so that IDs remain positive.

| Bits | Field | Range |
|---|---|---|
| 1 | sign (always 0) | — |
| 41 | timestamp in milliseconds since § 8.2. epoch | ~69 years |
| 10 | worker ID | 0..1023 |
| 12 | sequence within the same millisecond | 0..4095 |

Order from most to least significant: `sign | timestamp | worker | sequence`. IDs are therefore monotonically non-decreasing in time per worker.

## 8.2. Epoch

`2026-01-01T00:00:00Z`. Stored as a constant. Changing this constant invalidates all existing IDs and is forbidden after the first production write.

## 8.3. Worker ID configuration

| Source | Resolution order |
|---|---|
| Spring property `tldrtimes.snowflake.worker-id` | 1 (highest) |
| Environment variable `APP_WORKER_ID` (via property placeholder) | 2 |
| Default | `0` |

Operator responsibility: assign distinct worker IDs to distinct concurrent processes. Duplicate worker IDs across processes can produce identical Snowflake IDs and violate `BIGINT PRIMARY KEY`.

## 8.4. Clock skew

The generator tracks the last-issued millisecond. On `generateId()`:

| Condition | Action |
|---|---|
| `now > last` | Reset sequence to 0; emit. |
| `now == last` and sequence < 4095 | Increment sequence; emit. |
| `now == last` and sequence == 4095 | Busy-wait until the next millisecond; emit. |
| `now < last`, gap < 1000 ms | Busy-wait until `now >= last`; emit. |
| `now < last`, gap ≥ 1000 ms | Throw `IllegalStateException`. |

## 8.5. Hibernate integration

Two collaborating types in `common.domain.support`:

| Type | Role |
|---|---|
| `SnowflakeIdGenerator` (`@Component`) | Spring-managed bean. Holds `Clock`, worker ID, last-millisecond / sequence state. Exposes `synchronized long generateId()` and the static decoder of § 8.6. |
| `HibernateSnowflakeIdGenerator` (`implements org.hibernate.generator.BeforeExecutionGenerator`) | Hibernate plug-in. Constructed by Hibernate per entity field. Resolves the Spring `SnowflakeIdGenerator` bean via `ManagedBeanRegistry`, then delegates `generate(...)` to it. `getEventTypes()` returns `INSERT_ONLY`. |
| `@SnowflakeId` | Custom meta-annotation declared `@IdGeneratorType(HibernateSnowflakeIdGenerator.class)`. Field-level. Replaces the deprecated `@GenericGenerator` from Hibernate ≤ 6. |

Each entity declares its primary key as:

```java
@Id
@SnowflakeId
private Long id;
```

Entities never set `id` manually; Hibernate calls the generator before `INSERT` is executed (`BeforeExecutionGenerator` semantics) and writes the returned value into the `INSERT` statement.

## 8.6. Timestamp decoding

`SnowflakeIdGenerator` exposes a static decoder used by `BaseEntity` (§ 4.1.) and any other code that needs to recover creation time from an ID:

```java
public static Instant extractTimestamp(long id) {
    return Instant.ofEpochMilli(EPOCH_MS + (id >>> TIMESTAMP_SHIFT));
}
```

`TIMESTAMP_SHIFT = WORKER_BITS + SEQUENCE_BITS = 22`. The right-shift is unsigned (`>>>`) because the sign bit is always 0; signed shift would yield the same result here but `>>>` documents the invariant.

# 9. Resolved decisions

The questions tracked here in earlier drafts are now closed. The resolutions are folded into § 7. and reproduced for traceability.

| ID | Question | Decision | Where applied |
|---|---|---|---|
| Q1 | Generalize `rss_source` to `article_source`? | **Yes**; `ArticleSource` represents a provider-specific collection channel with `language` and `source_type`. | § 7.2.2. |
| Q2 | How is the initial `category` row (`football`) seeded? | **Deferred.** No seed migration was committed in this round. Population path (Flyway data migration, Spring `ApplicationRunner`, or operational `INSERT`) will be decided alongside the first ingestion run. | not yet applied |
| Q3 | Store original (untranslated) article title? | **No**. Operational identification of an article uses `article.source_url` and the loaded `article_summary.title` for the active reader language. | § 7.2.3. |
| Q4 | Add `processed_at`, `last_error_message` to `article_ingestion_job`? | **No** for now. Add when retry/observability work begins; not required for MVP correctness. | § 7.2.4. |

# 10. Glossary

| Term | Definition |
|---|---|
| Aggregate | A cluster of entities treated as a single consistency boundary, accessed only through its root entity. From Domain-Driven Design. |
| BCP-47 | IETF Best Current Practice 47, the standard for language tags (e.g. `en`, `ko-KR`). This project uses lowercase short forms. |
| Bean Validation | Jakarta standard for declarative input validation (`@NotNull`, `@Size`). |
| `BeforeExecutionGenerator` | Hibernate 7 SPI for generating values before the SQL statement executes. The Snowflake integration implements it for `INSERT_ONLY` event types. |
| bcrypt | Password hashing function producing 60-character output. |
| Flyway | Versioned SQL migration tool. Each `V{n}__name.sql` runs once in order. |
| HMAC-SHA256 | Keyed cryptographic hash. Used here to derive `ip_hash` from a per-deployment pepper and the client IP. |
| `@IdGeneratorType` | Hibernate 7 meta-annotation that binds a custom annotation (`@SnowflakeId` here) to a generator class, replacing the deprecated `@GenericGenerator`. |
| Idempotent | Producing the same end state when applied more than once. |
| `@MappedSuperclass` | JPA annotation for an inheritable class whose fields map into each subclass table; the superclass has no table of its own. |
| Package-by-feature | Organizing source code by domain concept (e.g. `article/`, `category/`) rather than by layer (`controller/`, `service/`). |
| Pepper | A secret value, distinct from a per-row salt, mixed into a hash to defeat rainbow tables. Stored in deployment config, not in the database. |
| `@PostLoad` / `@PostPersist` | JPA lifecycle callbacks invoked after a row is loaded or after an `INSERT` executes, respectively. Used here to populate the transient `createdAt` (§ 4.1.). |
| `@SQLRestriction` | Hibernate annotation that appends a fixed `WHERE` clause to every SELECT against an entity. Verified to propagate from a `@MappedSuperclass` to every concrete subclass at metamodel build time on Hibernate 7 (see § 3.5. and `SoftDeleteTest`). Compatible with `LAZY @ManyToOne` to soft-deletable targets, unlike `@SoftDelete`. |
| Snowflake | A 64-bit time-ordered identifier scheme (§ 8.). |
| Soft delete | Marking a row deleted by setting a column (here `deleted_at` to a timestamp) instead of removing it. SELECTs filter the column automatically via `@SQLRestriction` (§ 3.5.); the column is set in application code via `softDelete(Instant)` followed by `repository.save(...)`. |
| Testcontainers | A library that runs throwaway Docker containers from JUnit, used here to host MySQL during tests. |
| `@Transient` | JPA annotation marking a field as not persisted; the field exists in Java but no column is generated. |
