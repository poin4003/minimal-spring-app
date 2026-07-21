# Project Rules

## Scope
- Do not include `sample.sql` in the project's rule set.
- `sample.sql` is only a temporary file, not the source of truth for schema, migration, or naming conventions.
- This project targets a minimal stack: Spring Boot API + H2 + JobRunr.
- Do not include Redis, Kafka, or external services in this project's baseline.
- Initial migrations should stay minimal, focus on the current entities, and avoid seed data unless explicitly requested.

## Source Structure
- `src/main/java/com/app/config`: shared Spring configuration such as security, jwt, cors, swagger, mapper, exception, settings, and webmvc.
- `src/main/java/com/app/core`: shared components such as base entities, constants, response wrappers, security objects, sync infrastructure, and utilities.
- `src/main/java/com/app/features/<domain>`: code is organized by business domain.
- Inside each feature, prefer keeping these familiar directory groups:
  - `api/v1/controller`
  - `service` and `service/impl`
  - `repository` and `repository/spec` when dynamic filtering is needed
  - `entity`
  - `schema/payload`
  - `schema/result`
  - `schema/filter`
  - `enums`, `sync`, `worker`, `excel`, `cronjob` when the domain needs them
- `src/main/resources`: configuration files, logback, and Flyway migrations under `db/migration`.
- `src/test/java`: test code.

## Coding Direction
- Controllers should stay thin and mainly handle request input, validation, response mapping, and permission annotations.
- Business logic should live in the service layer.
- Database queries and criteria-based filtering should live in the repository/specification layer.
- Request/response/filter DTOs should live under `schema`.
- Shared constants should live under `core/constant`.
- New features should follow the `features/<domain>` structure instead of scattering code by technical layer across the whole project.
- APIs should keep versioning in the path, preferably `api/v1`.
- When the database schema changes, prefer creating a migration under `src/main/resources/db/migration`.
- Do not carry over tables, seed data, or integrations from the old project if the current codebase no longer uses them.
- Prefer explicit lambda expressions over Java method references such as `::`, especially in stream operations, sorting, and mapping.
- This project prefers lambdas because VS Code Java null-safety analysis tends to produce noisier warnings on method references than on equivalent lambdas.
- When both forms are valid, prefer styles such as `item -> item.getId()` or `item -> this.toView(item)` over `Type::method` or `this::method`.
- For flat DTOs or flat UI row views with matching field names, prefer mutable classes with `ModelMapper` over repetitive manual `builder()` mapping.
- Reserve manual builders for composed page objects, modal/detail structures, or cases where field names or mapping logic differ.

## Dependency Field Naming Rule
- Name injected repository fields with the domain name followed by `Repo`, such as `userBaseRepo`, `mediaRepo`, or `mediaVariantRepo`.
- Name injected service fields with the domain name followed by `Svc`, such as `userBaseSvc`, `mediaSvc`, or `authCookieSvc`.
- Apply this convention consistently in production code and tests.
- Keep repository and service class/interface names explicit; this abbreviation applies only to dependency field names and their references.

## UI View Model Rule
- For server-rendered UI pages such as Thymeleaf, prefer using dedicated view-model classes instead of scattering many `model.addAttribute("key", value)` entries.
- A controller should preferably expose one root page object, for example `page`, rather than multiple loose template variables.
- Shared UI data such as title, current user, logout path, and menu tree should be grouped into structured classes when appropriate.
- Thymeleaf templates should prefer reading structured fields such as `page.title`, `page.shell.currentUser.email`, or `page.shell.menuTree`.
- Framework-provided attributes such as `_csrf` are exceptions and may continue to be used directly.
- When building UI pages, prefer Java-style OOP view models over loose key-value model assembly.
- Separate business filter objects from paging and query-state objects; do not merge them into one DTO by default.
- Paging, sorting, and mode-switch state should prefer dedicated Java query classes over extending business filters for convenience.

## UI Component Reuse Rule
- When building server-rendered UI pages, prefer composing from shared reusable components instead of rewriting table, modal, and pagination markup per page.
- Shared UI components should stay centered around the common Java component models and factories such as `UiTable*`, `UiModal*`, and `UiPagination*`.
- If a new UI page needs a listing view, modal form, or paging bar, the default direction is to reuse these components first and only fall back to page-specific HTML when the shared component truly does not fit.
- For assignment-style UI flows such as assigning permissions to roles or roles to users, prefer shared page fragments and structured page models instead of pushing those workflows into heavy modals.
- Component inputs should also stay Java/OOP oriented, meaning annotated classes, structured view models, and factories are preferred over ad-hoc maps or loose template variables.
- UI descriptions, helper text, and table subtitles should describe the business function of the page or table, not mention internal implementation details such as shared or reusable components.

## UI/UX Rule
- For server-rendered UI pages, use modals only for simple flows such as create, update, metadata display, and lightweight detail display.
- If an action contains repeated state changes, larger datasets, or workflow-style navigation, prefer a dedicated page instead of a modal.
- Keep metadata display and detail display as separate UI concerns when they serve different purposes.
- If a page needs both metadata and related-item detail, prefer separate actions and separate modals instead of merging them into one heavy modal.
- For complex assignment screens, reuse the existing paging stack based on `BasePageFilter`, `UiPaginationPathBuilder`, and `UiPaginationFactory` instead of creating a separate UI-only paging system.
- Do not hide UI actions based on permissions at the Thymeleaf layer for now; let secured routes return `403` when the current user is not allowed.
- Do not use ad-hoc `Map<String, String>` structures for UI hidden fields, UI action payloads, or query-state transport.
- For Thymeleaf interaction state such as paging, sorting, mode switching, and post-back context, prefer dedicated Java classes such as `Ui...Query`, `Ui...Action`, or `Ui...State`.

## Exception Handling Rule
- Keep `details` and `fieldErrors` as separate properties in custom exceptions; do not overload `details` to carry field-level validation state.
- For server-rendered UI error handling, prefer dedicated page/view classes such as `ErrorPageView` and helper/factory classes such as `WebErrorPageFactory`.
- Avoid loose `ModelAndView.addObject(...)` chains for web error pages; prefer structured page objects passed as one root model attribute.
- When Thymeleaf pages need to surface backend business validation errors, prefer structured `fieldErrors` from `MyException` and a dedicated resolver/helper instead of mutating `BindingResult` from custom exceptions.

## Repository Query Rule
- Prefer Spring Data derived query methods for simple lookups, existence checks, relation traversal, and small delete operations.
- Prefer `@EntityGraph` for defining fetch plans instead of writing manual `JOIN FETCH` queries.
- Prefer `JpaSpecificationExecutor`, Criteria API, and the JPA static metamodel for dynamic filtering.
- Any general listing displayed in the UI or exposed as a browsable result must use `Page<T>` at the service boundary and accept paging through `Pageable` or the project's base paging filter.
- Do not use repository methods returning `List` or `Set` to implement general display listings.
- Repository methods returning `List` or `Set` are reserved for bounded domain operations, such as validating a caller-supplied collection of IDs, loading assignment relations, or performing finite business checks.
- Do not use `@Query` when the same behavior can be expressed clearly and efficiently with a derived query, entity graph, or specification.
- JPQL `@Query` is allowed for bulk update/delete operations or queries that cannot be expressed clearly without harming correctness or performance.
- Native SQL through `nativeQuery = true`, `@NativeQuery`, `EntityManager.createNativeQuery`, or JDBC query APIs is prohibited by default.
- Native SQL requires an explicit database-specific requirement and must be shown to the user before being added.
- Flyway schema and data migrations are exempt because SQL is their intended format.

## Audit Index Rule
- Every concrete entity extending `BaseAuditEntity` must declare table-specific indexes for both `created_at` and `updated_at`.
- Flyway migrations must create the matching audit indexes immediately after the related `CREATE TABLE` statement instead of grouping them at the end of the migration.
- Join tables without audit columns are excluded from this rule.

## Session Revocation Rule
- Use `@RevokeSessions` on service methods whose successful changes invalidate active JWT authorization state.
- Do not place `@RevokeSessions` on a broad update method when only some fields invalidate authentication state; split security-sensitive updates into explicit service methods.
- An annotated method must declare its target `UUID` as the first parameter so the aspect can bind it directly with `args(targetId, ..)`.
- Use `SessionRevocationScope.USER` when the first UUID is a user ID and `SessionRevocationScope.USERS_BY_ROLE` when it is a role ID.
- For role-scoped revocation, use one database bulk operation instead of loading affected user IDs into application memory.
- Revoke sessions before the domain method runs, then persist both changes in the same transaction so failures roll back together.
- Do not add parameter-level marker annotations, SpEL expressions, or reflection-based argument lookup for session revocation.

## Collaboration Rule With User
- Before changing code or files, the AI must show the code to the user first.
- Changes may only be applied after the user explicitly confirms by saying `apply`.
- If the user clearly says something like `apply now`, `apply directly`, or otherwise shows that the change should be made immediately, the AI may apply it in that same turn.
- The AI does not need to fully confirm every runtime case or edge case in advance; it only needs to show the code or proposed changes clearly so the user can decide the direction.
- This exception does not apply to reading source code, analysis, review, or proposing a fix.
- If implementation reveals a substantial source, schema, architecture, or infrastructure change outside the explicit prompt scope, the AI must pause and ask the user before proposing or applying that additional direction.
- Do not silently bundle large out-of-scope refactors or redesigns into an approved change. Small compile-safe adjustments that do not alter the agreed behavior remain allowed.

## Web API Integration Rule
- When browser JavaScript or a web frontend calls a backend API, treat the API integration as part of the complete web flow instead of implementing only the visual or client-side action.
- Explicitly handle the endpoint contract, authentication mechanism, authorization behavior, CORS or CSRF requirements, structured `ApiResult` responses, and API error handling relevant to that call.
- Prefer reusable JavaScript API helpers for shared authentication and error handling instead of scattering raw `fetch` behavior across individual pages or features.
- Thymeleaf controllers that call services directly are not required to introduce an API unless browser-side JavaScript actually needs one.

## Open Sections For Future Rules
- Naming conventions for entities, DTOs, endpoints, and repositories.
- Rules for transactions and validation.
- Rules for logging.
- Rules for tests.
- Rules for import/export and background jobs.
