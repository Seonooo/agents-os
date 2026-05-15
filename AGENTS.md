# Agent Instructions

This project is a TDD exercise. Failing test first → minimal implementation → refactor green.

## Project

- Purpose: MVP bulletin board. Scope: `GET /posts` (paged), `POST /posts`. No user authentication.
- Stack: Java 21, Spring Boot 4.0.6, Spring Data JPA, PostgreSQL, Gradle (Kotlin DSL), JUnit 5, Mockito, AssertJ, Testcontainers, Spring REST Docs.
- Build & test: `./gradlew test` (Docker must be running for Testcontainers).
- Entry: `src/main/java/com/example/board/BoardApplication.java`.
- Domain code: `src/main/java/com/example/board/post/`.
- Config: `src/main/resources/application.yml`.
- Migrations: Flyway under `src/main/resources/db/migration/V*.sql`.

## Conventions and rules

These are auto-loaded by Claude Code from `.claude/rules/`:

- `.claude/rules/conventions.md` — code conventions (records, DI, error contract, validation, pagination, testing, Jackson 3.x).
- `.claude/rules/hard-rules.md` — prohibitions (secrets, schema policy, dependency approval, file locks, MVP scope).

Other coding agents (Codex, Cursor, etc.) reading this `AGENTS.md` should also consult those two rule files.

## Task workflow

Each non-trivial task gets a context file:

1. Copy `.agent-os/context-template.md` to `.agent-os/tasks/YYYY-MM-DD-<slug>.md` and fill it.
2. AI reads `AGENTS.md` + the task context, then summarizes goal/scope/risks/acceptance and asks any questions.
3. Human confirms or corrects; corrections are recorded in `.agent-os/tasks/<slug>.review.md`.
4. Implementation proceeds via TDD.

## Extended guidance (load on demand)

Not auto-loaded — read explicitly when relevant (risky tasks, audits, onboarding):

- `docs/agents/principles.md` — working principles, role boundaries, tooling constraints, security details.
- `docs/agents/workflow.md` — 9-step task workflow + review checklist.
