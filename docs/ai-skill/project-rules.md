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

## Collaboration Rule With User
- Before changing code or files, the AI must show the code to the user first.
- Changes may only be applied after the user explicitly confirms by saying `apply`.
- If the user clearly says something like `apply now`, `apply directly`, or otherwise shows that the change should be made immediately, the AI may apply it in that same turn.
- The AI does not need to fully confirm every runtime case or edge case in advance; it only needs to show the code or proposed changes clearly so the user can decide the direction.
- This exception does not apply to reading source code, analysis, review, or proposing a fix.

## Open Sections For Future Rules
- Naming conventions for entities, DTOs, endpoints, and repositories.
- Rules for transactions and validation.
- Rules for logging.
- Rules for tests.
- Rules for import/export and background jobs.
