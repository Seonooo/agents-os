---
name: tdd-endpoint
description: Use when adding a new HTTP endpoint, refactoring an existing controller's request/response contract, or adding Bean Validation rules. Walks through the 4-round TDD pattern (Service unit → E2E happy path → validation failure cases → refactor + regression) established for this project on POST /posts, GET /posts, and the error-contract refactor.
---

# TDD Endpoint Workflow

This Skill formalizes the 4-round TDD pattern used three times on this project (POST /posts 2026-05-15, GET /posts 2026-05-16, error-contract refactor 2026-05-16). It complements `docs/agents/workflow.md` (the 9-step generic workflow) by giving an endpoint-specific recipe.

## When to use

- Adding any new HTTP endpoint (`@PostMapping`, `@GetMapping`, etc.).
- Refactoring an existing controller's request/response contract (e.g., changing error response shape across all endpoints).
- Adding Bean Validation rules to existing parameters or request bodies.

## When NOT to use

- Pure-domain changes with no HTTP surface (just service + repository) — use unit-only TDD without Round B/C.
- Schema-only migrations (no controller change) — write Flyway migration only.
- Build/config-only changes.

## Preconditions

Before starting Round A, confirm:
1. **Task Context exists** at `.agent-os/tasks/YYYY-MM-DD-<slug>.md` (filled from `.agent-os/context-template.md`).
2. **Acceptance Criteria** in the Task Context explicitly enumerates: success response shape, validation failure cases (per field), error response shape.
3. **Forbidden / Do not change** list reviewed — especially `application.yml` keys and file locks per `.claude/rules/hard-rules.md`.
4. **AI summary + human corrections** completed (see `<slug>.review.md`).

## Round A — Service unit

**Goal**: prove the domain operation works in isolation before any HTTP wiring.

- **Red**: write `<Domain>ServiceTest` in `src/test/java/.../post/`.
  - `@ExtendWith(MockitoExtension.class)`, `@Mock <Domain>Repository`.
  - Inject `Clock.fixed(...)` for deterministic timestamps.
  - For paginated reads: mock `repository.findAll(any(Pageable.class))` to return `new PageImpl<>(content, pageRequest, total)`.
  - Use `ArgumentCaptor<T>` to assert what the service passes to the repository (sort, page request, entity field values).
- **Green**: implement the service method.
  - Constructor injection only.
  - Read methods: `@Transactional(readOnly = true)`. Write methods: `@Transactional`.
  - Sort goes to the repository (`Sort.by(...)` or method name); never `list.sort(...)` in the service.

## Round B — E2E happy path

**Goal**: prove the full HTTP-to-DB path works with a single representative request.

- **Red**: write `<Domain>ControllerE2ETest`.
  ```java
  @SpringBootTest
  @AutoConfigureMockMvc  // org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
  @Import(TestcontainersConfiguration.class)
  class XControllerE2ETest {
      @Autowired private MockMvc mockMvc;
      @Autowired private ObjectMapper objectMapper;  // tools.jackson.databind.ObjectMapper (Jackson 3.x)
      @Autowired private XRepository repository;

      @BeforeEach
      void clearDatabase() { repository.deleteAll(); }
      // ...
  }
  ```
- **Green**: implement the controller method.
  - Thin: delegate to service immediately.
  - Return `ResponseEntity` only when you need custom status/headers (e.g., 201 + Location); plain return for 200 with body.
  - For schema-touching endpoints: add Flyway migration `src/main/resources/db/migration/V*.sql`. **Never** change `ddl-auto` (stays `validate`).
- **Verify**: `./gradlew test --tests "com.example.board.post.<Domain>ControllerE2ETest" --console=plain --no-daemon`.

## Round C — Validation / failure cases

**Goal**: every Acceptance-Criteria failure case has a test, and every error response follows the project's `ErrorResponse` + `ErrorCode` contract.

- **Red**: one test per failure case. Use a `private ResultActions performX(...)` helper to keep tests short. Use `expectValidationFailure(...)` helper to assert the common `ErrorResponse` shape:
  ```java
  .andExpect(status().isBadRequest())
  .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
  .andExpect(jsonPath("$.message").exists())
  .andExpect(jsonPath("$.path").value("/posts"))
  .andExpect(jsonPath("$.timestamp").exists())
  .andExpect(jsonPath("$.errors[*].field").value(hasItem("<field>")));
  ```
- **Green**:
  - Request body validation: `@NotBlank @Size(max=N)` on record fields, `@Valid` on `@RequestBody` parameter.
  - Query parameter validation: `@Min` / `@Max` directly on `@RequestParam` parameters.
  - **Do NOT** add `@Validated` on the controller class — that triggers the old Jakarta `MethodValidationInterceptor` path and throws `ConstraintViolationException` instead of the Spring 6.1+ MVC `HandlerMethodValidationException` the project's handler expects.
- Failures route through `RequestValidationExceptionHandler` (handles both `MethodArgumentNotValidException` and `HandlerMethodValidationException`).

## Round D — Refactor + regression

**Goal**: clean up duplication, confirm nothing else broke.

- Extract mapping duplication (e.g., `XResponse.from(XEntity)` static factory; `PageResponse.from(Page<T>)`).
- Run full regression: `./gradlew test --console=plain --no-daemon`.
- Walk `docs/agents/workflow.md` review checklist.
- Verify the diff is minimal — no unrelated cleanup bundled.

## Common Spring Boot 4 pitfalls (verify before guessing)

When something doesn't compile or doesn't behave as expected, **do not guess** — verify against `spring-projects/spring-boot` or `spring-projects/spring-framework` at the matching version tag. Patterns seen on this project:

- **Class moved**: `@AutoConfigureMockMvc` is at `org.springframework.boot.webmvc.test.autoconfigure` (not the old `*.web.servlet.*`).
- **Module split**: `spring-boot-flyway` is now a separate autoconfig module — having `flyway-core` on classpath alone doesn't trigger autoconfig.
- **Jackson 3.x**: `ObjectMapper` is at `tools.jackson.databind`. Annotations remain at `com.fasterxml.jackson.annotation`.
- **API renamed**: `HandlerMethodValidationException` uses `getParameterValidationResults()` — not `getAllValidationResults()`.
- **Property toggle**: `spring.mvc.problemdetails.enabled=true` activates Spring's default ProblemDetail. The project uses a custom `ErrorResponse` instead, so this is set but the handler overrides the response shape.

If a verification is needed, `gh search code 'public class XAutoConfiguration' --repo spring-projects/spring-boot --limit 3` is the fastest way to find a class's current location at a tag.

## Exit criteria

- All new tests green, full regression green (`./gradlew test`).
- Each Acceptance Criteria item has at least one corresponding test.
- `docs/agents/workflow.md` review checklist passes.
- The diff is minimal — no unrelated cleanup.
- `.agent-os/tasks/<slug>.review.md` updated if the implementation surfaced any new decision the AI summary didn't catch.

## References

- Task Context examples: `.agent-os/tasks/2026-05-15-post-create.md`, `2026-05-16-post-list.md`, `2026-05-16-error-contract.md`.
- Conventions: `.claude/rules/conventions.md`.
- Hard rules: `.claude/rules/hard-rules.md`.
- Workflow checklist: `docs/agents/workflow.md`.
