# 1. Swagger / OpenAPI Annotation Conventions

Swagger annotations from `io.swagger.v3.oas.annotations.*` (provided by springdoc-openapi, the Spring integration that emits OpenAPI specs) decorate Spring MVC controllers so the spec generator produces `docs/openapi.json` for downstream client type generation.

| Annotation | Target | Required attributes | Forbidden attributes |
| --- | --- | --- | --- |
| `@Tag` | controller class | `name` (group label, e.g. `"Articles"`) | `description` |
| `@Operation` | handler method | `operationId` shaped as `<verb><Resource>` (e.g. `listArticles`) | `summary`, `description` |
| `@Parameter` | parameter | only the case listed below | `description` |

`@Parameter` is permitted only in this case:

- **Hide a custom argument resolver.** A parameter bound by a `HandlerMethodArgumentResolver` (the Spring SPI that resolves controller arguments from the request) lacks a query/header/path classification, so springdoc may emit it as a spurious query parameter. Annotate with `@Parameter(hidden = true)`. Example target: `@ResolvedLanguage SupportedLanguage language`.

Headers consumed inside argument resolvers (e.g. `Accept-Language`) are intentionally not documented in the OpenAPI spec. Frontend clients send these headers based on out-of-band knowledge.

# 2. API Package Structure

An API package is a package under `com.everytldr.api` that owns Spring MVC controllers and API-use-case services for one externally visible resource.

| Rule | Required practice |
| --- | --- |
| Resource package | Put resource-specific controllers, services, and API exceptions under `com.everytldr.api.<resource>`. |
| Controller split | Split controllers by HTTP resource when one resource gains meaningful sub-resources. |
| Service split | Split services by use-case responsibility, not only by entity name. |
| Shared support | Put cross-cutting API infrastructure under `com.everytldr.api.support.<capability>`. |

Current article split: `ArticleController` owns article list/detail; `ArticleCommentController` owns `/api/articles/{articleId}/comments`; `ArticleLikeController` owns `/api/articles/{articleId}/likes/me`.

# 3. Controller Conventions

A controller is the HTTP boundary. It owns request mapping, request validation, custom argument-resolver parameters, service invocation, and response conversion. Business rules belong in services (§ 4.).

| Concern | Required practice |
| --- | --- |
| Route names | Use plural resource path segments, then `{id}` or a more specific nested identifier such as `{articleId}`. |
| Nested resource path variables | Prefer semantically specific names inside nested controllers, e.g. `articleId` for `/api/articles/{articleId}/comments`. |
| Service result conversion | Store service results in a local variable before calling `Response.from(...)`; do not inline service calls inside `from(...)`. |
| Custom argument resolver parameters | Annotate with `@Parameter(hidden = true)` as defined in § 1. |
| OpenAPI tag | Use the same `@Tag(name = "...")` for controllers that belong to the same public API group. |

# 4. Service Conventions

A service is an API-use-case component. It must not expose HTTP DTOs as input or output types.

| Rule | Required practice |
| --- | --- |
| HTTP DTO boundary | Do not accept `*Request` or return `*Response` from a service. |
| Small service result | If a result type is used only by one service and its controller, place it as a nested record in that service. |
| Entity return | Return entities only when the controller immediately maps them and the entity is not exposed directly as JSON. |
| Shared existence check | Keep shared resource existence checks in the service that owns the resource. |
| Transaction scope | Put write operations behind `@Transactional`; do not add read-only transactions unless the method needs a persistence-context guarantee. |

# 5. Request And Response DTO Conventions

A DTO is a data transfer object: a shape designed for moving data across the HTTP boundary.

| DTO kind | Required placement |
| --- | --- |
| Controller-only request | Nested record in the controller that owns the endpoint. |
| Controller-only response | Nested record in the controller that owns the endpoint. |
| List item | Nested `Item` record inside the list response. |
| Nested item schema | Add `@Schema(name = "...")` to nested `Item` records to prevent OpenAPI schema-name collisions. |
| Reused or large DTO | Separate file in the same resource package. |

Repository projections are not DTOs. Do not return repository projections directly from controllers; convert them to response DTOs (§ 6.).

# 6. Repository Projection Conventions

A repository projection is a query result shape optimized for persistence access. It is not an API contract.

| Rule | Required practice |
| --- | --- |
| Placement | Put query-specific projections as nested records in the repository that creates them. |
| JPQL constructor expression | Reference nested projection records with the JVM binary name using `$`. |
| API exposure | Do not serialize repository projections directly from controllers. |
| Naming | Use names that describe the query result, e.g. `DetailProjection` or `ListItemProjection`. |

# 7. Exception Conventions

An API exception is a service-layer failure that `ApiExceptionHandler` maps to an HTTP status.

| Rule | Required practice |
| --- | --- |
| Placement | Use resource-level container classes, e.g. `ArticleExceptions` or `ArticleCommentExceptions`. |
| Service nesting | Do not put exceptions inside service classes. |
| HTTP mapping | Map API exceptions in `ApiExceptionHandler`. |
| Framework exception | Do not throw `ResponseStatusException` from services. |

# 8. Profile Configuration Conventions

A profile-specific property is a configuration property used only when a Spring profile is active.

| Rule | Required practice |
| --- | --- |
| Common property | Put properties used by multiple profiles in `application.yaml`. |
| API-only property | Put properties used only by the `api` profile in `application-api.yaml`. |
| Ingestor-only property | Put properties used only by the `ingestor` profile in `application-ingestor.yaml`. |

Example: `everytldr.client-address.hash-secret` belongs in `application-api.yaml` because `ClientAddressConfig` is annotated with `@Profile("api")`.

# 9. Client Address Conventions

A client address is the request-origin address resolved by API infrastructure from trusted proxy headers.

| Rule | Required practice |
| --- | --- |
| Resolver parameter | Use `@ResolvedClientAddress ClientAddress` only in controllers. |
| Raw IP storage | Do not persist raw IP addresses. |
| Stable hash | Persist only HMAC-derived hashes where a stable per-client key is required. |
| Display value | Use `maskedIp` for public comment display. |
| Missing IP | Fail closed with `ClientAddressExceptions.Unavailable`. |

# 10. Test Conventions

A controller test verifies the HTTP contract. A service test verifies business behavior independent of HTTP.

| Test kind | Required scope |
| --- | --- |
| Controller test | Use `MockMvc` to verify route, status, request validation, response JSON, exception mapping, and custom argument-resolver behavior. |
| Service test | Verify business rules, persistence mutations, idempotence, and domain-specific failure modes. |
| Repository test | Verify query semantics only when the query is complex enough to justify direct persistence testing. |

Controller tests must stay minimal. A class named `*ControllerTest` must exercise the controller through the HTTP boundary; it must not call repositories or services as the primary assertion target. Do not test repository ordering, cursor internals, or query joins through controller tests unless the HTTP contract directly depends on the exact visible result.

Common test support is allowed only when it removes repeated Spring wiring or fixture setup. Remove it if it hides test intent or accumulates unrelated helpers.
