# Skill Candidates Tracker

> **Rule** (from Notion / Anthropic best practices): when a prompt or workflow is reused **2 or more times**, it becomes a Skill candidate. When count ≥ 2 **and** the pattern is stable, extract to `.claude/skills/<name>/SKILL.md`.
>
> This file is the **meta-process tracker**. Not auto-loaded by Claude Code (lives under `docs/agents/`). Update when you notice repetition.

## Status legend

- 🟢 **extracted** — lives at `.claude/skills/<name>/SKILL.md`, model-invoked via Progressive Disclosure
- 🟡 **candidate** — count ≥ 2, pending extraction (decide if pattern is stable first)
- ⚪ **monitoring** — count = 1, watch for second occurrence
- ⚫ **rejected** — won't be a skill (too project-specific or already covered by a rule)

## Active candidates

| Pattern | Count | Last seen | Status | Skill path |
|---|---|---|---|---|
| TDD 4-round for new HTTP endpoint | 3 | 2026-05-16 | 🟢 extracted | `.claude/skills/tdd-endpoint/` |
| Spring Boot 4 API verification (renamed/moved classes) | 4 | 2026-05-16 | 🟢 extracted | `.claude/skills/spring-boot-4-verify/` |
| `gh search code` / `gh api` for Spring source | 5+ | 2026-05-16 | 🟢 merged into `spring-boot-4-verify` | (same) |
| Task Context + AI summary + review.md flow | 3 | 2026-05-16 | 🟢 extracted | `.claude/skills/task-context-flow/` |
| Hibernate column naming verification (snake_case mapping) | 1 | 2026-05-16 | ⚪ monitoring | — |
| Testcontainers Postgres seed-then-GET test data setup | 2 | 2026-05-16 | 🟡 candidate (mostly absorbed in `tdd-endpoint` Round B) | — |
| Atomic counter via `@Modifying @Query` + `clearAutomatically` | 1 | 2026-05-16 | ⚪ monitoring | — |
| Domain exception → `@ExceptionHandler` → `ErrorResponse` mapping | 1 | 2026-05-16 | ⚪ monitoring | — |
| Soft delete via `@SQLRestriction` + `@Modifying @Query` explicit filter | 1 | 2026-05-16 | ⚪ monitoring (gotcha documented in `.claude/rules/conventions.md`) | — |
| Mutable domain methods on JPA entity (`update`, `delete`) instead of setters | 1 | 2026-05-16 | ⚪ monitoring (rule in `.claude/rules/conventions.md`) | — |
| Flyway V2+ migration (additive ALTER without index) | 1 | 2026-05-16 | ⚪ monitoring | — |
| Reusing existing domain exception across new endpoints (no new exception class) | 1 | 2026-05-16 | ⚪ monitoring | — |

## Detailed history

### TDD 4-round for new HTTP endpoint
**Count: 3 / Status: 🟢 extracted**

- 2026-05-15 — `POST /posts`: 4 rounds (Red unit → Green entity·svc → Red E2E → Green ctrl·Flyway·Clock → Refactor regression). See `.agent-os/tasks/2026-05-15-post-create.md`.
- 2026-05-16 — `GET /posts` (paged): Round A unit (Pageable + sort) → Round B E2E happy → Round C validation (`@Min/@Max` + `HandlerMethodValidationException`) → Round D refactor. See `.agent-os/tasks/2026-05-16-post-list.md`.
- 2026-05-16 — error contract refactor: Red 6 POST validation tests → Green `ErrorResponse` + `ErrorCode` + handler refactor. See `.agent-os/tasks/2026-05-16-error-contract.md`.

→ **Extracted to `.claude/skills/tdd-endpoint/SKILL.md`** (2026-05-16).

### Spring Boot 4 API verification
**Count: 4 / Status: 🟢 extracted**

Pattern: when SB4 throws a `ClassNotFoundException`, `package does not exist`, or `cannot find symbol`, look up the class in spring-projects/spring-boot or spring-projects/spring-framework GitHub at the matching version tag to find the moved/renamed counterpart.

- 2026-05-15 — Flyway autoconfig: `org.flywaydb:flyway-core` alone doesn't trigger SB4 autoconfig — need `spring-boot-flyway` module (autoconfig split in SB4).
- 2026-05-15 — `@AutoConfigureMockMvc` moved: `org.springframework.boot.test.autoconfigure.web.servlet.*` → `org.springframework.boot.webmvc.test.autoconfigure.*`.
- 2026-05-15 — Jackson 3.x: `com.fasterxml.jackson.databind.ObjectMapper` → `tools.jackson.databind.ObjectMapper` (annotations remain at `com.fasterxml.jackson.annotation.*`).
- 2026-05-16 — `HandlerMethodValidationException` API: not `getAllValidationResults()`, actual API is `getParameterValidationResults()`.

→ **Extracted to `.claude/skills/spring-boot-4-verify/SKILL.md`** (2026-05-16). Includes the migration catalog above plus the verification recipe (`gh search code`, `gh api`, Spring Initializr cross-check, WebFetch on docs).

### Task Context + AI summary + review.md flow
**Count: 3 / Status: 🟢 extracted**

The "fill template → AI summarizes + asks → human corrects → record in `.review.md` → implement" cycle.

- 2026-05-15 — `POST /posts` (Q1~Q6 + Q-A/B/C resolved).
- 2026-05-16 — `GET /posts` (pagination correction recorded as Major correction in review).
- 2026-05-16 — error contract (E1~E5 resolved).

→ **Extracted to `.claude/skills/task-context-flow/SKILL.md`** (2026-05-16). Captures the 7-step ritual and the I/Q ambiguity-marker convention.

### `gh search code` / `gh api` for Spring source inspection
**Count: 5+ / Status: 🟢 merged into `spring-boot-4-verify`**

- BOM testcontainers entry lookup
- HandlerMethodValidationException source
- FlywayAutoConfiguration location
- JacksonAutoConfiguration location
- spring-boot-flyway module build.gradle

→ Merged: these are the primary discovery tools used by `spring-boot-4-verify` Skill (Recipe Step 1 & 2).

### Hibernate column naming verification
**Count: 1 / Status: ⚪ monitoring**

- 2026-05-16 — confirmed `SpringPhysicalNamingStrategy` default maps `createdAt → created_at`, `viewCount → view_count`.

→ Watch for second occurrence.

### Atomic counter via `@Modifying @Query` + `clearAutomatically`
**Count: 1 / Status: ⚪ monitoring**

- 2026-05-16 — `PostRepository.incrementViewCount(id)` for GET `/posts/{id}` viewCount. Pattern: `@Modifying(clearAutomatically = true) @Query("UPDATE ... SET counter = counter + 1 WHERE id = :id")`. Returns affected row count for not-found detection. `clearAutomatically` ensures subsequent `findById` returns the post-UPDATE state (read-your-write).

→ Watch for second occurrence (likes, replies, view-by-day counter, etc.). Extract at count=2.

### Soft delete via `@SQLRestriction` + `@Modifying @Query` explicit filter
**Count: 1 / Status: ⚪ monitoring**

- 2026-05-16 — `Post` entity gets `@SQLRestriction("deleted_at IS NULL")` for auto-filtering all entity reads. `PostRepository.incrementViewCount` HQL `@Modifying @Query` adds `AND p.deletedAt IS NULL` manually (because `@SQLRestriction` does NOT apply to HQL/JPQL modifying queries). DELETE endpoint becomes `findById -> post.delete(now)` (dirty checking sets `deleted_at`). V2 Flyway migration adds nullable column.

→ Watch for second domain needing soft delete (Comment, User, etc.). Extract at count=2 as a comprehensive soft-delete Skill (entity annotation + repository query rules + DELETE service pattern + V2 migration template).

### Mutable domain methods on JPA entity instead of setters
**Count: 1 / Status: ⚪ monitoring**

- 2026-05-16 — `Post.update(title, content, updatedAt)` and `Post.delete(deletedAt)` added as domain methods (no setters exposed). Used by `PostService.update/delete` via dirty checking (no explicit `repo.save`).

→ Watch for second entity needing mutability. Extract at count=2 as a Skill on "JPA entity domain method discipline".

### Flyway V2+ migration (additive ALTER)
**Count: 1 / Status: ⚪ monitoring**

- 2026-05-16 — `V2__add_posts_deleted_at.sql` adds a nullable column. Notable: no index added (kept minimal per Hard rules — only PK guaranteed; non-PK indexes need explicit justification later).

→ Watch for second migration to see if a Flyway-migration Skill (template + verification checklist) is worth extracting.

### Reusing existing domain exception across new endpoints
**Count: 1 / Status: ⚪ monitoring**

- 2026-05-16 — `PostNotFoundException` + `RESOURCE_NOT_FOUND` originally introduced for GET-by-id (2026-05-16 earlier), reused unchanged by PUT and DELETE endpoints in the same day. Pattern: keep one exception class per "missing resource" semantic, attach multiple call sites — do NOT create per-endpoint exception subclasses.

→ Watch for second reuse case (next domain). Extract at count=2 as a Skill on "exception reuse vs subclassing decision tree".

### Domain exception → `@ExceptionHandler` → `ErrorResponse` mapping
**Count: 1 / Status: ⚪ monitoring** (this entry tracks CREATION of new exception+handler+code; reuse is tracked separately above)

- 2026-05-16 — `PostNotFoundException` → `handlePostNotFound` in `RequestValidationExceptionHandler` → `ErrorResponse(code = RESOURCE_NOT_FOUND, ...)`. Pattern: custom exception class + `@ExceptionHandler(X.class)` method on existing advice + new `ErrorCode` enum value.

→ Watch for second occurrence (DuplicatePostException, BusinessRuleViolation, etc.). Extract at count=2.

### Testcontainers Postgres seed-then-GET test data setup
**Count: 2 / Status: 🟡 candidate**

The "save N entities via `repository.saveAll(...)` with distinct `Instant` values, then verify GET response order/contents" pattern.

- 2026-05-16 — empty page + multi-row sort + custom size on `GET /posts`.
- 2026-05-16 — error contract tests (POST validation only, but uses same `@BeforeEach clearDatabase()` + `repository`)

→ Mostly absorbed by the TDD-endpoint Skill's Round B section. If a third occurrence introduces distinct shape, extract separately.

## Update protocol

1. When you notice a pattern repeated, increment its count + add to "Detailed history" with date and reference.
2. When a candidate reaches count ≥ 2 **and** the pattern feels stable (won't keep changing shape), extract to `.claude/skills/<name>/SKILL.md`.
3. After extraction, update Status → 🟢 and add the Skill path.
4. If a candidate ends up never recurring (count stuck at 1 for a long time), mark ⚫ rejected with reason.

## Reference

- Notion: "두 번째로 같은 프롬프트를 친다 싶으면 → Skill 후보로 적어둠"
- [Anthropic — Equipping agents for the real world with Agent Skills](https://www.anthropic.com/engineering/equipping-agents-for-the-real-world-with-agent-skills)
- [Skill authoring best practices (공식)](https://platform.claude.com/docs/en/agents-and-tools/agent-skills/best-practices)
