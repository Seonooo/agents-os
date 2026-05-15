# Task Context

## Goal

`GET /posts/{id}` 엔드포인트로 단일 게시글의 상세를 조회한다. 조회 시 `viewCount`를 정확히 +1 증가시키고, 존재하지 않는 id에 대해서는 새 `ErrorCode.RESOURCE_NOT_FOUND`로 404를 반환한다.

## Background

POST `/posts`(2026-05-15)·페이지네이션 GET `/posts`(2026-05-16)에 이은 MVP 게시판의 세 번째 엔드포인트. 두 선행 작업에서 `viewCount` 증가 로직은 의도적으로 미뤘다 (`Forbidden: viewCount mutation`) — 이번 작업이 그 정식 처리 자리.

부수적으로 두 가지 신규 패턴이 처음 도입된다:
- `ErrorCode` enum에 두 번째 값(`RESOURCE_NOT_FOUND`) 추가 — 에러 카탈로그 확장 첫 사례.
- atomic UPDATE 쿼리 (`UPDATE posts SET view_count = view_count + 1 WHERE id = ?`) — 동시성 안전 패턴 도입.

## Related Files / Areas

**변경 후보 (will touch)**
- `src/main/java/com/example/board/post/PostController.java` — `@GetMapping("/{id}")` 메서드 추가
- `src/main/java/com/example/board/post/PostService.java` — `find(Long id)` 메서드 추가, 증가 + 조회 트랜잭션 1개
- `src/main/java/com/example/board/post/PostRepository.java` — `@Modifying @Query` 메서드 추가 (atomic increment, `clearAutomatically = true`)
- `src/main/java/com/example/board/post/ErrorCode.java` — `RESOURCE_NOT_FOUND` 추가
- `src/main/java/com/example/board/post/PostNotFoundException.java` — **신규** 도메인 예외 (10줄 이내)
- `src/main/java/com/example/board/post/RequestValidationExceptionHandler.java` — `handlePostNotFound(...)` 메서드 추가
- `src/test/java/com/example/board/post/PostServiceTest.java` — `find` 단위 테스트
- `src/test/java/com/example/board/post/PostControllerE2ETest.java` — GET-by-id E2E 테스트

**참고용 (reference only)**
- `src/main/java/com/example/board/post/Post.java` — 엔티티 (필드 변경 없음, 시그니처 변경 없음)
- `src/main/java/com/example/board/post/PostResponse.java` — 응답 record 그대로 재사용
- `src/main/resources/db/migration/V1__create_posts.sql` — 스키마 변경 없음

**도메인 영역**: `com.example.board.post`

## Existing Patterns

POST/GET 작업에서 확립된 패턴 그대로:

- 생성자 주입, 필드 주입 금지 (`.claude/rules/conventions.md`)
- DTO는 `record` — `PostResponse` 7필드 재사용
- JPA 엔티티는 클래스
- Lombok 미사용
- 컨트롤러 얇게, 비즈니스 규칙은 서비스에
- 단위 테스트: JUnit 5 + Mockito + AssertJ
- E2E: `@SpringBootTest` + `@AutoConfigureMockMvc` + `@Import(TestcontainersConfiguration.class)` + Testcontainers Postgres
- Jackson 3.x: `tools.jackson.databind.*`
- 에러 응답: 커스텀 `ErrorResponse` + `ErrorCode`
- Spring 6.1+ MVC 검증: `@Validated` 클래스 어노테이션 사용 금지

신규 패턴 (이번에 도입):
- atomic UPDATE 쿼리로 카운터 증가 (race condition 회피)
- 도메인 예외 (`PostNotFoundException`) → `RequestValidationExceptionHandler` 확장으로 매핑

## Change Scope

Can change:

- `PostController.java` — `@GetMapping("/{id}")` 1개 추가
- `PostService.java` — `find(Long id)` public 메서드 추가, `@Transactional` (read+update 한 트랜잭션)
- `PostRepository.java` — `@Modifying @Query` 메서드 1개 추가
- `ErrorCode.java` — `RESOURCE_NOT_FOUND` 한 줄 추가
- `PostNotFoundException.java` — 신규 (1개 파일, 10줄 이내)
- `RequestValidationExceptionHandler.java` — `handlePostNotFound(...)` 신규 메서드 추가
- `PostServiceTest.java`, `PostControllerE2ETest.java` — 테스트 메서드 추가

Do not change:

- `Post.java` — 엔티티 시그니처 그대로
- `PostResponse.java`, `PostCreateRequest.java`, `PageResponse.java`
- `BoardApplication.java`, `application.yml`, `build.gradle.kts`
- `src/main/resources/db/migration/**` — 스키마 변경 없음
- `support/TestcontainersConfiguration.java`, `BoardApplicationTests.java`
- 기존 POST/GET 테스트 메서드들
- `RequestValidationExceptionHandler`의 기존 `handleMethodArgumentNotValid` / `handleHandlerMethodValidationException` 메서드 본문

## Constraints

- **Security**: PII 없음. 응답에 stack trace 노출 금지. 404 응답은 단순 `RESOURCE_NOT_FOUND` (DB 내부 정보 노출 X).
- **Performance**: atomic UPDATE + SELECT = 2 round-trip. 인덱스는 PK뿐이라 둘 다 인덱스 lookup. 응답 < 50ms 로컬 기준.
- **Operations**: Hibernate SQL 로그에 `UPDATE posts SET view_count = view_count + 1 WHERE id = ?` 와 후속 `SELECT ... FROM posts WHERE id = ?` 가 나옴 (format_sql: true).
- **Compatibility**: 기존 POST·GET·init 17개 테스트 모두 그대로 통과 (회귀 금지). `PostResponse` JSON 계약 변경 없음.
- **Time**: 학습 일정 내. 정확한 마감 없음.

## Acceptance Criteria

### 성공 응답
- `GET /posts/{id}` 요청 시 (id가 존재):
  - HTTP `200 OK`
  - 응답 body는 `PostResponse` 7필드 (`id, author, title, content, createdAt, updatedAt, viewCount`)
  - **응답의 `viewCount`는 이 호출 후의 값** (즉 호출 전 0이었으면 응답은 1)

### viewCount 정책
- 매 GET 호출마다 정확히 +1 (세션·IP 무관, 작성자 본인 호출도 카운트)
- 동시 N회 호출 → 정확히 +N (atomic UPDATE로 보장)
- POST·GET-list는 증가시키지 않음 (기존 정책 유지)

### 404 응답
- 존재하지 않는 id (예: `/posts/99999`, `/posts/-1`) → HTTP `404 Not Found`
- 응답 body는 `ErrorResponse(code = RESOURCE_NOT_FOUND, message = ..., path = "/posts/{id}", timestamp = ..., errors = null)`

### 잘못된 id 형식 응답
- 비숫자 id (예: `/posts/abc`) → HTTP `400 Bad Request` (Spring 기본 `MethodArgumentTypeMismatchException` 그대로). 본문 형식은 검증 안 함 (Spring 기본 `ProblemDetail`).
- 음수 id (예: `/posts/-1`) → HTTP `404 Not Found` (DB에 존재 안 함이라는 자연스러운 처리)

### 회귀
- 기존 17개 테스트 모두 통과
- `PostResponse` JSON 형식 불변

## Verification

- **Tests to run**:
  - `./gradlew test --tests "com.example.board.post.*"`
  - 전체: `./gradlew test`
- **Logs to check**:
  - Hibernate: `UPDATE posts SET view_count = view_count + 1 WHERE id = ?` + `SELECT ... FROM posts WHERE id = ?`
  - 404 응답에 stack trace 없음
- **API/UI to check**:
  ```bash
  # 글 1개 작성
  curl -X POST http://localhost:8080/posts \
       -H "Content-Type: application/json" \
       -d '{"author":"alice","title":"Hello","content":"World"}'
  # 응답에서 id 추출 (가령 1)

  # 첫 조회 — viewCount=1
  curl -i http://localhost:8080/posts/1
  # 두 번째 조회 — viewCount=2
  curl -i http://localhost:8080/posts/1

  # 존재하지 않는 id
  curl -i http://localhost:8080/posts/99999
  # 기대: 404, body: {"code":"RESOURCE_NOT_FOUND", "message":..., "path":"/posts/99999", ...}

  # 비숫자 id
  curl -i http://localhost:8080/posts/abc
  # 기대: 400 (Spring 기본 형식)
  ```

## Forbidden

- 새 의존성 추가 금지
- 새 Flyway 마이그레이션 추가 금지 (스키마 변경 없음 — `viewCount` 컬럼은 V1에 이미 존재)
- `Post` 엔티티 필드 추가/변경 금지
- `PostResponse` 필드 구성 변경 금지 (POST/GET 응답 계약과 공유)
- POST/GET이 `viewCount`를 증가시키도록 변경 금지 (정책 유지)
- 단순 incrementation에 `@Version` (optimistic locking) 도입 금지 — atomic UPDATE로 충분
- 응답 헤더 `Cache-Control` 추가 금지 (MVP 범위 외)
- 작성자 본인 호출을 식별·차감하는 로직 도입 금지 (인증 없음, 식별 수단 없음)
- 비동기 / fire-and-forget viewCount 처리 도입 금지 (MVP에선 동기·트랜잭션 내)
- 기존 `RequestValidationExceptionHandler.handleMethodArgumentNotValid` / `handleHandlerMethodValidationException` 메서드 본문 변경 금지 (404 핸들러는 신규 메서드로 추가)
- `application.yml`의 `ddl-auto`, `format_sql` 변경 금지
