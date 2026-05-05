# 1. Swagger / OpenAPI Annotation Conventions

Swagger annotations from `io.swagger.v3.oas.annotations.*` (provided by springdoc-openapi, the Spring integration that emits OpenAPI specs) decorate Spring MVC controllers so the spec generator produces `docs/openapi.json` for downstream client type generation.

| Annotation | Target | Required attributes | Forbidden attributes |
| --- | --- | --- | --- |
| `@Tag` | controller class | `name` (group label, e.g. `"Articles"`) | `description` |
| `@Operation` | handler method | `operationId` shaped as `<verb><Resource>` (e.g. `listArticles`) | `summary`, `description` |
| `@Parameter` | parameter or method | only the cases listed below | `description` |

`@Parameter` is permitted only in these cases:

- **Hide a custom argument resolver.** A parameter bound by a `HandlerMethodArgumentResolver` (the Spring SPI that resolves controller arguments from the request) lacks a query/header/path classification, so springdoc may emit it as a spurious query parameter. Annotate with `@Parameter(hidden = true)`. Example target: `@ResolvedLanguage SupportedLanguage language`.
- **Register an implicit header.** A header read inside a resolver (e.g. `Accept-Language`, the IETF BCP 47 language-preference header) binds to no controller parameter, so springdoc cannot detect it. Add `@Parameter(in = ParameterIn.HEADER, name = "...", required = false)` at the method level.
