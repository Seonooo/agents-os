# Hard Rules

These are project-wide prohibitions. Violating any of them is a bug.

## Secrets and data

- No secrets, credentials, API keys, or PII in code, tests, fixtures, logs, or commits.
- Test fixtures use clearly fake values: `alice`, `bob`, `carol`, `Lorem ipsum`, etc.
- Treat `application*.yml` containing credentials as off-limits to commit. Use env vars or gitignored `application-local.yml`.

## Schema and persistence

- **No schema change without a new Flyway migration** under `src/main/resources/db/migration/V*.sql`.
- **Never modify** `spring.jpa.hibernate.ddl-auto` in `application.yml`. It stays `validate`.
- **Never modify** `spring.jpa.properties.hibernate.format_sql`.
- V1 migration has PK only — additional indexes require V2+.

## Dependencies

- **No new dependency** in `build.gradle.kts` without explicit human approval. Justify the dependency, then ask.
- Do not downgrade Spring Boot or change Java version.

## File locks (do not edit)

These files are intentionally frozen. Hard enforcement is mirrored in `.claude/settings.local.json` `permissions.deny`.

- `gradlew`, `gradlew.bat`, `gradle/wrapper/**` — Gradle wrapper. Regenerate via `gradle wrapper` only.
- `src/test/java/com/example/board/support/TestcontainersConfiguration.java` — shared test infra.
- `src/test/java/com/example/board/BoardApplicationTests.java` — init smoke test (regression guard).
- `settings.gradle.kts`, `.gitignore` — leave alone unless explicitly asked.

## Out of MVP scope

Do not introduce any of these without an explicit task:

- Authentication / authorization
- Pagination/search/filter beyond the existing `?page`, `?size`
- `viewCount` mutation (POST/list do not increment)
- Post update / delete endpoints
- Spring REST Docs snippet generation
