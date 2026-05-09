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
