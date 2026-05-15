---
name: spring-boot-4-verify
description: Use when Spring Boot 4 throws "package does not exist", "cannot find symbol", "could not find org.X:Y:", autoconfig not loading, or any class/property name from prior memory looks suspicious. Spring Boot 4 split many autoconfig classes into per-feature modules and reorganized packages — never guess the new location, always verify via gh search on spring-projects repos or WebFetch on the official docs. This Skill provides the verification recipe plus a catalog of confirmed migrations from this project's history.
---

# Spring Boot 4 API Verification

This project pins **Spring Boot 4.0.6** (with Spring Framework 7, Jackson 3.x, JPA via `spring-boot-starter-data-jpa`). The 4.x release made structural changes that break mechanical memory from 3.x:

- Autoconfig was split out of `spring-boot-autoconfigure` into per-feature modules (`spring-boot-flyway`, `spring-boot-jackson`, etc.).
- Test starters got their own packages (`org.springframework.boot.webmvc.test.autoconfigure.*` instead of `org.springframework.boot.test.autoconfigure.web.servlet.*`).
- Jackson 3.x moved core types to `tools.jackson.databind.*`.
- Some method names on framework exceptions changed.

The rule: **never guess; always verify against `spring-projects/spring-boot` (or `spring-projects/spring-framework`) at the tag matching the pinned version**.

## When to use this Skill

Trigger when any of the following happens:
- Compiler: `package X does not exist`, `cannot find symbol`, `class X is deprecated`.
- Gradle: `Could not find org.X:Y:` (no version), or `Could not resolve` for a Spring artifact.
- Runtime: a Spring autoconfig that should run silently does nothing (e.g., no Flyway migration log, no `ObjectMapper` bean autowired).
- A class/method name from memory feels off — even before the error appears.

## Verification recipe

Order: **GitHub source > official docs > guessing-with-disclosure**. Never the last alone.

### 1. Locate the class on GitHub at the matching tag

```bash
# Find where a class lives
gh search code 'public class HandlerMethodValidationException' \
  --repo spring-projects/spring-framework --limit 3

# Find a class by interface signature
gh search code 'public @interface AutoConfigureMockMvc' \
  --repo spring-projects/spring-boot --limit 3

# Find by string mention (broader)
gh search code 'FlywayAutoConfiguration' \
  --repo spring-projects/spring-boot --limit 5
```

### 2. Read the file at the exact version tag

```bash
# Spring Boot 4.0.6 BOM (find a managed-dependency entry)
gh api repos/spring-projects/spring-boot/contents/platform/spring-boot-dependencies/build.gradle?ref=v4.0.6 \
  --jq '.content' 2>/dev/null | base64 -d | grep -A 3 -i 'library("Testcontainers"'

# An autoconfig class's source
gh api repos/spring-projects/spring-boot/contents/module/spring-boot-flyway/src/main/java/org/springframework/boot/flyway/autoconfigure/FlywayAutoConfiguration.java?ref=v4.0.6 \
  --jq '.content' 2>/dev/null | base64 -d | head -50

# A starter's actual dependency declaration
gh api repos/spring-projects/spring-boot/contents/starter/spring-boot-starter-webmvc/build.gradle?ref=v4.0.6 \
  --jq '.content' 2>/dev/null | base64 -d | head -40
```

### 3. Check the official docs for properties / behavior toggles

WebFetch on:
- `https://code.claude.com/docs/en/memory` (when checking Claude Code memory itself)
- `https://docs.spring.io/spring-boot/appendix/dependency-versions/coordinates.html` (managed dependency coordinates)
- `https://github.com/spring-projects/spring-boot/releases` (release tag dates and notes)

### 4. Cross-check with Spring Initializr's actual generation

When unsure about the canonical starter coordinates or the recommended dependency set, fetch a reference project from Spring Initializr:

```powershell
Invoke-WebRequest -Uri 'https://start.spring.io/starter.zip?type=gradle-project-kotlin&language=java&bootVersion=4.0.6&...' `
  -OutFile $env:TEMP\boot-init.zip
Expand-Archive -Path $env:TEMP\boot-init.zip -DestinationPath $env:TEMP\boot-init
Get-Content $env:TEMP\boot-init\demo\build.gradle.kts
```

This is what we used to discover that `spring-boot-starter-web` is now `spring-boot-starter-webmvc` and the new test starter split.

## Catalog of confirmed migrations (this project, Week 1–2)

These are the verified findings from real failures on this project. Use them as a first-pass lookup table before the recipe above.

| Surprise | Old (training memory) | New (verified) | Date |
|---|---|---|---|
| MVC starter | `spring-boot-starter-web` | `spring-boot-starter-webmvc` | 2026-05-15 |
| Test starters | `spring-boot-starter-test` (umbrella) | Per-area: `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`, `spring-boot-starter-validation-test`, `spring-boot-starter-restdocs` | 2026-05-15 |
| Testcontainers artifacts | `org.testcontainers:junit-jupiter`, `org.testcontainers:postgresql` | `org.testcontainers:testcontainers-junit-jupiter`, `org.testcontainers:testcontainers-postgresql` (also: PostgreSQLContainer class moved from `org.testcontainers.containers` to `org.testcontainers.postgresql`) | 2026-05-15 |
| Flyway autoconfig | `org.flywaydb:flyway-core` alone triggered autoconfig | Need `org.springframework.boot:spring-boot-flyway` module (autoconfig split). `flyway-core` is brought in transitively. | 2026-05-15 |
| Jackson core | `com.fasterxml.jackson.databind.ObjectMapper` | `tools.jackson.databind.ObjectMapper` (Jackson 3.x). Annotations remain at `com.fasterxml.jackson.annotation.*`. | 2026-05-15 |
| `@AutoConfigureMockMvc` package | `org.springframework.boot.test.autoconfigure.web.servlet` | `org.springframework.boot.webmvc.test.autoconfigure` | 2026-05-15 |
| `HandlerMethodValidationException` API | `getAllValidationResults()` (does not exist) | `getParameterValidationResults()` | 2026-05-16 |
| `@Validated` on controller class | Triggers method validation via Jakarta → `HandlerMethodValidationException` | Triggers OLD Jakarta `MethodValidationInterceptor` path → `ConstraintViolationException`. For new Spring 6.1+ MVC method validation, **omit** `@Validated` and put constraints directly on `@RequestParam`. | 2026-05-16 |
| ProblemDetail default body | Includes per-field errors | Empty body unless `spring.mvc.problemdetails.enabled=true`. Even then, no field details — need custom `@ControllerAdvice`. | 2026-05-15 |

## Patterns to expect (beyond the catalog)

When verifying something new:
- **"X moved to per-feature module"**: search for `module/spring-boot-X/` directory at the version tag.
- **"X.Y class renamed"**: check `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` files in the relevant module to see canonical class names.
- **"property name changed"**: check `module/spring-boot-X/src/main/java/.../<X>Properties.java` for `@ConfigurationProperties("...")`.

## Anti-patterns

- ❌ Pattern-matching from Spring Boot 2.x or 3.x memory.
- ❌ Trying random imports until something compiles.
- ❌ Assuming an autoconfig will run because the underlying library is on classpath.
- ❌ Claiming "this should work in Spring Boot 4" without a citation.

## Exit criteria

- The verified class/property/method has a concrete reference: file path at `spring-projects/spring-boot@v4.0.6` (or `spring-framework@v7.X.X`), or an official docs URL.
- The catalog above is updated with the new finding (PR a line into this Skill).
- If the surprise is also a project-wide convention, mirror it into `.claude/rules/conventions.md` so future code is written correctly from the start.

## References

- `.claude/rules/conventions.md` — confirmed conventions live here for always-loaded access.
- `docs/agents/skill-candidates.md` — tracks how often this verification pattern recurs.
- [Spring Boot release page](https://github.com/spring-projects/spring-boot/releases) — release dates and tag names.
- [Spring Boot managed dependency coordinates](https://docs.spring.io/spring-boot/appendix/dependency-versions/coordinates.html).
