# Code Conventions

## Language & DTOs

- Java 21 `record` for DTOs: `PostCreateRequest`, `PostResponse`, `ErrorResponse`, `PageResponse<T>`.
- JPA entities are classes (record incompatible with JPA).
- **No Lombok.** Write explicit getters/constructors.

## Wiring

- Constructor injection only. No `@Autowired` on fields.
- Thin controllers; business rules go in services.
- Read methods use `@Transactional(readOnly = true)`.
- Time-dependent logic uses an injected `Clock` (bean in `BoardApplication`).

## Error handling

- Use custom `ErrorResponse` record + `ErrorCode` enum. **Do not** use Spring's `ProblemDetail` directly.
- Validation failures: `code = VALIDATION_FAILED`, with `errors[].field` listing violated parameter or body field names.
- `ErrorResponse` is JSON-serialized as `application/json` (not `application/problem+json`).

## Validation (Spring 6.1+)

- For controller method parameters (`@RequestParam`), put `@Min/@Max/@NotBlank` etc. directly on the parameter. Spring's built-in MVC method validation triggers automatically.
- **Do not** annotate the controller class with `@Validated` — that routes through the old Jakarta `MethodValidationInterceptor` and throws `ConstraintViolationException` instead of the new `HandlerMethodValidationException`.
- For `@RequestBody`, use `@Valid` on the parameter.

## Pagination

- Use custom `PageResponse<T>` record (`{content, page, size, totalElements, totalPages}`). **Never** serialize Spring's `Page<T>` directly to JSON.
- Server fixes the sort: `Sort.by(Order.desc("createdAt"), Order.desc("id"))`. **No `sort` query parameter** is exposed.
- `size` clamped to `[1, 100]`, default 20. `page >= 0`, default 0. Out-of-range → 400.

## Persistence

- Postgres via Spring Data JPA. `findAll(Pageable)` for paged queries.
- IDs use `Long` + `GenerationType.IDENTITY` (Postgres `BIGSERIAL`).
- Time stored as `Instant` → Postgres `TIMESTAMP WITH TIME ZONE`.
- Naming: rely on Spring's default `SpringPhysicalNamingStrategy` (camelCase → snake_case).
- **Soft delete**: use Hibernate `@SQLRestriction("deleted_at IS NULL")` at entity class level. Auto-filters every entity-based read (`findById`, `findAll(Pageable)`, derived methods, etc.).
- **GOTCHA — `@Modifying @Query` does NOT inherit `@SQLRestriction`.** Any HQL/JPQL `UPDATE` or `DELETE` query targeting a soft-deletable entity must explicitly add `AND p.deletedAt IS NULL` (or the equivalent column predicate). Forgetting this lets writes mutate logically-deleted rows.
- Soft-deletable entities expose a `delete(Instant)` **domain method**, not a setter. Service layer pattern: `findById(id).orElseThrow(...Exception::new).delete(clock.instant())` — JPA dirty checking issues the UPDATE on transaction commit. Never call `repository.deleteById(id)` on a soft-deletable entity (that performs hard delete).
- Mutable domain state on JPA entities goes through named domain methods (`post.update(...)`, `post.delete(...)`), never raw setters.

## Testing

- Unit: JUnit 5 + Mockito + AssertJ. Mock the repository layer.
- E2E: `@SpringBootTest` + `@AutoConfigureMockMvc` + `@Import(TestcontainersConfiguration.class)` against real Postgres via Testcontainers.
- Fixed-clock unit tests: pass a `Clock.fixed(...)` into `PostService` constructor.

## Jackson 3.x (Spring Boot 4)

- Use `tools.jackson.databind.ObjectMapper` (Jackson 3 package). **Not** `com.fasterxml.jackson.databind.*`.
- Jackson annotations are still in `com.fasterxml.jackson.annotation.*` (e.g., `@JsonInclude`).

## Build & API docs

- Gradle Kotlin DSL (`build.gradle.kts`). Spring Boot 4 starters: `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa-test`, `spring-boot-starter-webmvc-test`, etc. (new split modules).
- Spring REST Docs starter is on classpath but snippet generation is deferred (not part of MVP).
