# Task Context

## Goal

`POST /posts/{postId}/comments` 한 endpoint를 추가한다. 신규 Comment entity + V3 Flyway 마이그레이션 + 생성 경로(Controller/Service/Repository/DTO)까지만 포함한다. 검증 실패·Post 미존재 시 기존 error contract를 그대로 재사용한다.

## Background

지금까지 Post 한 리소스에 대한 CRUD + 검색만 존재한다. nested resource 패턴을 처음 도입하는 task. core/project agent-os split 이후 loop.md step 5 ("small implementation plan")을 정직하게 지키기 위해 두 endpoint(POST/GET)를 분리, 본 task는 **POST만**.

후속 task `2026-05-XX-post-comments-list` 가 GET 목록 endpoint를 entity·repository 재사용으로 추가할 예정. 따라서 본 task에서 만든 Comment entity·schema는 후속 task의 기반이 된다 — schema 결정(Q-E, Q-F)은 양 task 모두를 고려해서 내려야 한다.

직전 post-search retro의 action item "테스트 실패가 잘못된 task context 가정을 발견했다 (R-2)"를 의식하여, 본 task에서는 framework 동작에 대한 가정(JPA `@ManyToOne` 기본 fetch, `existsById`의 `@SQLRestriction` 적용 여부)을 lock-in 테스트로 명시.

## Related Files / Areas

**신규 (create)**
- `src/main/java/com/example/board/comment/Comment.java` — JPA 엔티티
- `src/main/java/com/example/board/comment/CommentRepository.java` — `JpaRepository<Comment, Long>` (이 task에선 `save`만 사용; `findByPostId`는 후속 task)
- `src/main/java/com/example/board/comment/CommentService.java` — `create(Long postId, CommentCreateRequest)`만
- `src/main/java/com/example/board/comment/CommentController.java` — `POST` 메서드만
- `src/main/java/com/example/board/comment/CommentCreateRequest.java` — record DTO
- `src/main/java/com/example/board/comment/CommentResponse.java` — record DTO
- `src/test/java/com/example/board/comment/CommentServiceTest.java` — 단위 (Mockito): create 성공, post 미존재 시 예외
- `src/test/java/com/example/board/comment/CommentControllerE2ETest.java` — E2E + REST Docs: 생성 성공, 검증 실패(2~3건), post 미존재, body 누락
- `src/main/resources/db/migration/V3__create_comments.sql` — 신규 테이블

**참고 (reference only, 변경 없음)**
- `Post.java`, `PostRepository.java` — Post 존재 확인용으로만 호출 (`existsById`)
- `PageResponse.java` — 이번 task에서는 안 씀 (목록 endpoint가 없음). 후속 task에서 재사용 예정
- `ErrorResponse.java`, `ErrorCode.java` — 그대로 재사용
- `PostNotFoundException.java` — Post 미존재 시 재사용
- `RequestValidationExceptionHandler.java` — 기존 핸들러로 자동 흡수 (Comment 전용 핸들러 추가 없음)
- `BoardApplication.java`, `application.yml` — 패키지 스캔 범위(`com.example.board`)가 새 패키지 자동 포함

**도메인 영역**: `com.example.board.comment` (신규 패키지, Post와 sibling layout — Q-A 확정)

## Existing Patterns

기존 컨벤션 그대로 (Post 라인업에서 확립, `.claude/rules/conventions.md`):

- DTO는 record (`CommentCreateRequest`, `CommentResponse`)
- 엔티티는 class + `protected` no-arg 생성자 + 공개 도메인 생성자 + named domain methods
- 생성자 주입, 필드 `@Autowired` 금지
- 쓰기 메서드 `@Transactional`
- 시간은 주입된 `Clock`으로 (`clock.instant()`)
- ID `Long` + `GenerationType.IDENTITY` (Postgres `BIGSERIAL`)
- 시간 `Instant` → `TIMESTAMP WITH TIME ZONE`
- Validation: `@RequestBody`는 `@Valid`, `@PathVariable`은 별도 검증 안 함 (존재 확인은 Service)
- 검증 실패 → 400 `VALIDATION_FAILED` + `errors[].field`
- 404 → `RESOURCE_NOT_FOUND` (`PostNotFoundException` 재사용)
- TDD 4-round (`tdd-endpoint` Skill)
- E2E는 Testcontainers Postgres + REST Docs

**Soft-delete 컨벤션 (Comment에 적용 여부는 Q-F)**:
- `@SQLRestriction("deleted_at IS NULL")` 엔티티 레벨
- `delete(Instant)` 도메인 메서드, setter 금지
- `@Modifying @Query`에는 자동 적용 안 됨 → 명시 (본 task에는 해당 안 됨, save만 씀)

## Change Scope

Can change:

- 신규 패키지 `com.example.board.comment` 8개 파일
- 신규 Flyway 마이그레이션 `V3__create_comments.sql` (테이블 + FK + `post_id` 인덱스. `deleted_at` 컬럼은 V4로 분리 — Q-F 확정)

Do not change:

- `Post.java`, `PostResponse.java` — Post 응답에 `commentCount` 추가 금지
- `PostController.java`, `PostService.java`, `PostRepository.java` — Post API 변경 0
- `PageResponse.java`, `ErrorResponse.java`, `ErrorCode.java`, `PostNotFoundException.java`, `RequestValidationExceptionHandler.java` — 재사용만, 변경 0
- `application.yml`, `build.gradle.kts` — 의존성 추가 금지, 설정 변경 금지
- `db/migration/V1__create_posts.sql`, `V2__add_posts_deleted_at.sql` — frozen
- `support/TestcontainersConfiguration.java`, `BoardApplicationTests.java` — frozen
- 기존 43개 E2E + 기존 단위 테스트 — 회귀로 통과 확인만

## Constraints

- **Security**:
  - 사용자 입력은 JPA 파라미터 바인딩 (SQL injection 차단)
  - `author`, `content` 길이 상한으로 oversized input 차단
  - PII 없음 (`alice`, `bob`, `Lorem ipsum`)
  - 인증/인가 없음 — 누구나 어떤 post에든 댓글 가능
- **Performance**:
  - Post 존재 확인은 `existsById(postId)` 한 번 (`findById` 전체 row 로딩 회피)
  - INSERT 한 번. 본 task에 SELECT-루프 가능성 없음 (N+1 없음)
  - V3 마이그레이션에 `CREATE INDEX comments_post_id_idx ON comments (post_id)` 포함 (Q-E 확정). 본 task INSERT 빈도 < 후속 GET 조회 빈도 → 미리 비용 차단.
- **Operations**:
  - Hibernate SQL: `SELECT 1 FROM posts WHERE id=? AND deleted_at IS NULL` (existsById) + `INSERT INTO comments (...) VALUES (...)`
  - 400/404 응답은 기존 핸들러로 자동 흡수
  - `format_sql: true` 그대로
- **Compatibility**:
  - 기존 Post API 응답 바이트-동일
  - 기존 43개 E2E + 단위 테스트 그대로 통과
  - REST Docs 기존 스니펫 모두 재생성 가능
- **Time**:
  - endpoint 1개지만 신규 entity + 마이그레이션이 있어 post-search보다 약간 큼. TDD 4-round 예상 약 2~3시간. 길어지면 후속 task로 더 잘게 쪼갠다.

## Acceptance Criteria

### POST `/posts/{postId}/comments` — 생성 성공

- 요청: `Content-Type: application/json`, body `{"author": "alice", "content": "nice post"}`
- 응답: `201 Created`
  - Header `Location: /posts/{postId}/comments/{commentId}` (resource locator 의미만 — GET-by-id endpoint는 본 task scope 아님)
  - Body: `CommentResponse {id, postId, author, content, createdAt, updatedAt}`
- DB: `comments` row 1건 INSERT, `created_at == updated_at == clock.instant()`

### POST 검증 실패 (각 항목 독립 400)

- `author` 빈 문자열 → 400 + `errors[].field="author"` (NotBlank)
- `author` 31자 → 400 + `errors[].field="author"` (Size)
- `content` 빈 문자열 → 400 + `errors[].field="content"`
- `content` 1001자 → 400 + `errors[].field="content"` (`@Size(max=1000)` — Q-D 확정)
- body 누락 / 잘못된 JSON → 400 (Spring 기본 `HttpMessageNotReadableException` — 기존 contract)

### POST — Post 미존재

- 존재하지 않는 `postId` → 404 + `code=RESOURCE_NOT_FOUND` (`PostNotFoundException` 재사용, `existsById` false)
- soft-deleted Post의 id → 404 (`@SQLRestriction`이 `existsById`도 자동 처리)

### Soft-delete (Comment 자체) — 본 task scope 아님

- Q-F 확정: **V4로 분리**. V3에 `deleted_at` 컬럼 없음, 엔티티에 `@SQLRestriction` 없음, `delete(Instant)` 도메인 메서드 없음.
- 후속 delete task에서 V4 마이그레이션 + 엔티티 annotation + endpoint를 한 묶음으로 추가 (Post의 V1→V2 패턴과 대칭).

### 회귀

- 기존 43개 E2E + 단위 테스트 전부 통과
- POST·GET·GET-by-id·PUT·DELETE Post API 동작 불변
- REST Docs 기존 스니펫 모두 재생성 가능 + Comment POST 스니펫 신규 생성

## Verification

- **Tests to run**:
  - `./gradlew test --tests "com.example.board.comment.*"`
  - 전체: `./gradlew test`
- **Logs to check**:
  - 성공: `SELECT 1 FROM posts WHERE id=? AND deleted_at IS NULL` + `INSERT INTO comments (...) VALUES (...)`
  - 404: post 존재 확인 SELECT 한 번만 (INSERT 실행 안 됨)
- **API/UI to check**:
  ```bash
  # seed post
  curl -X POST http://localhost:8080/posts -H "Content-Type: application/json" \
       -d '{"author":"alice","title":"First Post","content":"hello"}'
  # → 201, id=1 가정

  # 댓글 생성
  curl -i -X POST http://localhost:8080/posts/1/comments -H "Content-Type: application/json" \
       -d '{"author":"bob","content":"nice"}'
  # 기대: 201, Location: /posts/1/comments/1, body=CommentResponse

  # 검증 실패 (author 빈)
  curl -i -X POST http://localhost:8080/posts/1/comments -H "Content-Type: application/json" \
       -d '{"author":"","content":"hi"}'
  # 기대: 400, errors[].field="author"

  # post 미존재
  curl -i -X POST http://localhost:8080/posts/9999/comments -H "Content-Type: application/json" \
       -d '{"author":"bob","content":"hi"}'
  # 기대: 404

  # body 누락
  curl -i -X POST http://localhost:8080/posts/1/comments -H "Content-Type: application/json"
  # 기대: 400
  ```

## Forbidden

- 새 의존성 추가 금지
- Post API 응답·요청 계약 변경 금지 (`PostResponse`에 `commentCount` 추가 금지)
- **GET `/posts/{postId}/comments` (목록) endpoint 본 task에서 추가 금지** — 후속 task 전용
- Comment 업데이트 / 삭제 endpoint 본 task에서 추가 금지 (Q-F가 컬럼만 미리 깔도록 허용해도 endpoint는 금지)
- Threaded replies (`parent_comment_id`) 추가 금지
- Comment에 별도 ErrorCode (`COMMENT_NOT_FOUND` 등) 추가 금지 — `RESOURCE_NOT_FOUND` + `PostNotFoundException` 재사용
- `Comment` ↔ `Post` 양방향 매핑 금지 (Post에 `@OneToMany List<Comment>` 추가 금지)
- JPA cascade / orphanRemoval 사용 금지
- 인증/인가 도입 금지
- 캐싱 / 비동기 / 스트리밍 도입 금지
- `application.yml` `ddl-auto: validate` 변경 금지
- Native SQL 금지 (V3 마이그레이션 외)
- Spring Data `Specification`, QueryDSL 등 신규 추상화 도입 금지

## Resolved Decisions

| Q | 결정 | 한 줄 근거 |
|---|---|---|
| Q-A | 패키지 = `com.example.board.comment` (sibling) | 향후 Comment를 다른 리소스로 확장 시 패키지 이동 회피. `trade-off-table` Skill로 비교. |
| Q-D | `@Size(max=1000)` | Post content 5000보다 짧은 댓글 직관. 잠정안 그대로. |
| Q-E | V3에 `CREATE INDEX comments_post_id_idx ON comments (post_id)` 포함 | 신규 테이블 초기 인덱스는 hard-rules "V1 PK only" 제약 대상 아님. 후속 GET 조회 비용 미리 차단. |
| Q-F | **V4로 분리** — V3에 `deletedAt` 컬럼 없음, 엔티티 annotation 없음, domain method 없음 | Post V1→V2 패턴과 대칭. 각 task가 정확히 자기 것만 schema 건드림. |
| Q-G | `private Long postId` (plain FK 컬럼, `@ManyToOne` 사용 안 함) | 본 task 사용 빈도 INSERT 한 곳뿐. 단순성·N+1 위험 0이 navigability보다 우선. |

## Deferred to list task (`post-comments-list`)

- **Q-B** 댓글 정렬 (`createdAt ASC/DESC`)
- **Q-C** GET에서 post 미존재 시 응답 (`404` vs `200 + 빈 배열`)
- **Q-H** REST Docs 스니펫 범위

## Interpretive points (AI가 task 채울 때 한 가정)

- **I1**: URL 패턴은 nested resource `/posts/{postId}/comments`. flat `/comments?postId=`는 부적합.
- **I2**: POST 응답 body에 `postId` 포함. URI에 이미 있지만 클라이언트 편의를 위해 본문에도 echo.
- **I3**: `updatedAt` 컬럼/필드는 일관성 위해 포함 (현재 update endpoint 없어도). 생성 시 `createdAt == updatedAt`.
- **I4**: Comment-전용 NotFoundException 추가 안 함. 404의 유일한 원인은 Post 미존재.
- **I5**: 단위 테스트는 Service 레벨, E2E는 Controller 레벨. Repository 단독 테스트 안 함.
- **I6**: 동시 INSERT는 IDENTITY ID 생성으로 안전, 별도 lock 미도입.
- **I7**: `existsById`는 Spring Data 기본 메서드로 `@SQLRestriction` 자동 적용 (회귀 lock-in 테스트로 검증).
