# Task Context

## Goal

모든 클라이언트 오류 응답(현재 4xx 범위)을 단일 `ErrorResponse` record로 통일하고, `ErrorCode` enum으로 오류 유형을 식별 가능하게 만든다. Spring의 `ProblemDetail`(`application/problem+json`)을 완전히 대체한다. 이 계약이 도입된 뒤 GET /posts 작업이 동일 계약 위에서 페이지네이션 파라미터 검증을 구현한다.

## Background

현재 POST 400 응답은 `ProblemDetail`에 `errors[]`를 얹은 형태이며, 클라이언트는 **오류 유형을 프로그램적으로 식별할 수 없다**(generic `"title":"Bad Request"` + 자유 텍스트뿐). GET /posts 작업(2026-05-16) 검토 중 사용자가 "error code + ErrorResponse로 어떤 에러인지 알 수 있어야 한다"고 결정 — 그 결정의 선행 조건이 이 작업.

이 작업의 결과로 POST의 6개 validation E2E 테스트도 새 계약에 맞춰 갱신된다. GET 작업은 이 작업 완료 후 재개된다.

## Related Files / Areas

**변경 후보 (will touch)**
- `src/main/java/com/example/board/post/ErrorResponse.java` — **신규** record
- `src/main/java/com/example/board/post/ErrorCode.java` — **신규** enum
- `src/main/java/com/example/board/post/RequestValidationExceptionHandler.java` — `ProblemDetail` 반환을 `ErrorResponse`로 교체
- `src/test/java/com/example/board/post/PostControllerE2ETest.java` — 6개 validation 메서드(`missing_author_returns_400_with_field_name`, `whitespace_only_title_returns_400`, `missing_content_returns_400`, `oversized_author_returns_400`, `oversized_title_returns_400`, `oversized_content_returns_400`)의 assertion 갱신

**참고용 (reference only)**
- `src/main/java/com/example/board/post/PostController.java`, `PostCreateRequest.java` — 변경 없음
- `src/main/java/com/example/board/post/PostService.java`, `Post.java`, `PostRepository.java`, `PostResponse.java` — 변경 없음
- `src/test/java/com/example/board/post/PostServiceTest.java` — 변경 없음
- `src/test/java/com/example/board/BoardApplicationTests.java` — 변경 없음

**도메인 영역**: 웹 계층 (cross-cutting)

## Existing Patterns

`POST /posts` 작업(2026-05-15)에서 확립된 패턴을 그대로 따른다:

- DTO·응답은 Java 21 `record`
- Lombok 미사용
- 생성자 주입, 필드 주입 금지
- 단위 테스트: JUnit 5 + Mockito + AssertJ
- E2E: `@SpringBootTest` + Testcontainers Postgres
- Jackson은 3.x(`tools.jackson.databind`)

새로 도입하는 패턴:
- 응답 record는 정적 팩토리(`ErrorResponse.of(...)`)로 생성
- `Instant` 직렬화는 Jackson 기본 ISO-8601

## Change Scope

Can change:

- `RequestValidationExceptionHandler.java` — 반환 타입과 형식 전면 교체
- `ErrorResponse.java`, `ErrorCode.java` — 신규 생성
- `PostControllerE2ETest.java`의 **6개 validation 메서드 본문** — assertion 부분만 갱신
- (선택) `PostControllerE2ETest.java`에 검증용 helper 메서드 추가

Do not change:

- `PostController.java`, `PostService.java`, `PostCreateRequest.java`, `PostResponse.java`, `Post.java`, `PostRepository.java`
- `application.yml` — `spring.mvc.problemdetails.enabled: true`는 그대로 유지 (Spring 내부 다른 경로에 영향 가능성 차단)
- `build.gradle.kts`, `BoardApplication.java`
- `support/TestcontainersConfiguration.java`, `BoardApplicationTests.java`
- `PostServiceTest.java`, `PostControllerE2ETest.java`의 **happy path 메서드** (`valid_request_returns_201_with_location_and_body`)
- `src/main/resources/db/migration/**`

## Constraints

- **Security**: ErrorResponse에 stack trace·내부 경로·시크릿 노출 금지. `path`는 요청 URI만(query string 제외).
- **Performance**: 핸들러 경로는 예외 케이스 — 성능 SLO 없음.
- **Operations**: 로그에 ErrorResponse가 정상 직렬화되는지 확인.
- **Compatibility**: 이 작업이 끝난 시점에 전체 9개 테스트가 새 계약으로 통과해야 한다. POST 400 응답 모양이 바뀌므로, **외부 클라이언트가 있다면 깨짐** — MVP 단계라 외부 클라이언트 없음을 전제로 진행.
- **Time**: 학습 일정 내. 정확한 마감 없음.

## Acceptance Criteria

### ErrorResponse record 구조
```java
public record ErrorResponse(
    ErrorCode code,
    String message,
    String path,
    Instant timestamp,
    List<FieldError> errors
) {
    public record FieldError(String field, String message) {}
}
```
- `code`: `ErrorCode` enum 값 (JSON 직렬화 시 `"VALIDATION_FAILED"` 같은 name)
- `message`: 사람이 읽을 1줄 요약 (예: `"Request validation failed"`)
- `path`: 요청 URI (`/posts`) — query string 제외
- `timestamp`: 응답 생성 시점 `Instant` (서버 시간, UTC, ISO-8601)
- `errors`: 필드별 검증 실패 상세. 검증 외 오류라면 `null` + `@JsonInclude(NON_NULL)`로 JSON 직렬화 시 생략

### ErrorCode enum
- 초기 값: `VALIDATION_FAILED` (Bean Validation 본문 검증)
- 추후 다른 오류 유형이 생기면 enum에 추가 (예: `RESOURCE_NOT_FOUND`, `INVALID_PARAMETER_TYPE`)
- JSON 직렬화는 enum name 그대로 (`"VALIDATION_FAILED"`)

### Handler 동작
- `MethodArgumentNotValidException` (POST body validation) → `400` + `ErrorResponse(VALIDATION_FAILED, ..., errors=[{field, message}, ...])`
- `HandlerMethodValidationException` (`@RequestParam @Min/@Max` 등) 핸들러는 **이 작업에서 다루지 않음** — GET /posts 작업에서 실제 endpoint와 함께 TDD로 추가 (AGENTS.md "테스트 없이 프로덕션 코드 금지" 원칙)
- 응답 `Content-Type`: `application/json`

### POST 6개 validation 테스트 갱신
- `status().isBadRequest()` 그대로
- `jsonPath("$.code").value("VALIDATION_FAILED")` 추가
- `jsonPath("$.errors[*].field").value(hasItem("author"))` 같은 기존 assertion은 새 구조와 호환되도록 동일 패턴 유지

### 회귀
- 전체 9개 테스트(init 1 + service 1 + happy POST 1 + validation 6) 모두 통과
- 단, validation 6개는 본 작업에서 assertion이 갱신됨

## Verification

- **Tests to run**:
  - `./gradlew test --tests "com.example.board.post.*"`
  - `./gradlew test` (전체)
- **Logs to check**:
  - ErrorResponse 직렬화 시 `Instant`가 ISO-8601 문자열로 나오는지
- **API/UI to check**:
  ```bash
  # 잘못된 요청 → 새 형식
  curl -i -X POST http://localhost:8080/posts \
       -H "Content-Type: application/json" \
       -d '{"author":"alice","content":"World"}'
  # 기대: 400, Content-Type: application/json
  #       body: {
  #         "code": "VALIDATION_FAILED",
  #         "message": "Request validation failed",
  #         "path": "/posts",
  #         "timestamp": "2026-...",
  #         "errors": [{"field":"title","message":"..."}]
  #       }
  ```

## Forbidden

- 새 의존성 추가 금지
- POST 성공 응답 `PostResponse` 변경 금지 (성공 계약은 그대로)
- POST 도메인 로직(`PostService.create`, `Post` 엔티티 등) 변경 금지
- `PostControllerE2ETest.valid_request_returns_201_with_location_and_body` 변경 금지 (happy path 회귀 가드)
- `PostServiceTest`, `BoardApplicationTests` 변경 금지
- ErrorResponse를 성공 응답에 끼워넣기 금지 (오류 전용)
- GET /posts 구현 시작 금지 — 별도 후속 작업
- `application.yml`의 `ddl-auto`, `format_sql` 변경 금지
- 시크릿·stack trace·내부 경로를 ErrorResponse에 노출 금지
- ErrorCode enum 값으로 `OK`, `SUCCESS` 같은 비-오류 값 추가 금지
